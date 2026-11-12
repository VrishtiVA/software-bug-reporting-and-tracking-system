
# -------------------- Database Connection Configuration --------------------
# This database user will have READ-ONLY access to the MySQL database.

# If you are trying to run the project locally, 
# you can update these database connection parameters if necessary.

USERNAME = "db_user"
PASSWORD = "password"
HOST = "localhost"
PORT = "3306"
DBNAME = "va104db"
DBNAME_TEST = "va104db_test"

# ---------------------------------------------------------------------------

import os
from sqlalchemy import create_engine, text
import numpy as np
import torch
import datetime

# Handle switch to test database if provided "test" environment variable.
DBENV = "test" if os.getenv("ENV", "").lower() == "test" else "prod"

# Using SQLAlchemy
engine = create_engine(f'mysql+pymysql://{USERNAME}:{PASSWORD}@{HOST}:{PORT}/{DBNAME if DBENV == "prod" else DBNAME_TEST}')

# Initial connection to test that connection is possible on application atart
# Using with to ensure that the connection is returned to the connection pool to be reusable once done here.
with engine.connect() as conn:
    try:
        conn.execute(text("SELECT 1"))
    except Exception as e:
        print("Unable to query database in duplicate detection microservice: ", e)

def fetchReportEmbeddingTimestampAndVectorEmbedding(report_id: int) -> tuple[datetime.datetime, torch.Tensor]:
    """
    Function to fetch embedding timestamp and vector embedding of a report from the MySQL database.

    :param report_id: The report ID to fetch by
    :type report_id: int
    """
    report_embedding = []
    embedding_timestamp = None
    with engine.connect() as conn:
        try:
            results = conn.execute(text("""
                SELECT embedding_timestamp, vector_embedding 
                FROM report
                WHERE id = :report_id
            """), {
                "report_id": report_id
            })
            try:
                for (et, em) in results:
                    embedding_timestamp = et
                    # Convert vector embedding back from bytes to tensor too
                    report_embedding.append(np.frombuffer(em, dtype=np.float32))
                report_embedding = torch.from_numpy(np.stack(report_embedding))
            except Exception as e2:
                print("Failed to decode current report embedding:", e2)
        except Exception as e:
            print("Failed to fetch current report creation timestamp and embedding:", e)
        finally:
            return embedding_timestamp, report_embedding

def fetchCandidateReports(workspace_id: int, report_id: int, embedding_timestamp: datetime.datetime) -> tuple[list, torch.Tensor]:
    """
    Function to fetch candidate reports from the MySQL database.
    This includes reports that are
    - not this report AND
    - have an embedding AND
    - was embedded before this report if not yet contained AND
    - not from closed containers.
    
    :param report_id: The report ID
    :type report_id: int
    :param workspace_id: The workspace ID
    :type workspace_id: int
    :param embedding_timestamp: The timestamp for when the vector embedding for this report was created.
    :type embedding_timestamp: int
    """
    indexed_report_ids = []
    embeddings = []
    with engine.connect() as conn:
        try:
            results = conn.execute(text("""
                SELECT r.id, r.vector_embedding 
                FROM report AS r 
                LEFT JOIN report_container AS rc on rc.id = r.report_container_id
                WHERE 
                    r.workspace_id = :workspace_id AND
                    r.id != :report_id AND 
                    r.duplicate_detection_lifecycle_state != 'NEW' AND
                    (rc.progress IS NULL OR rc.progress != 'CLOSED') AND 
                    (
                        r.duplicate_detection_lifecycle_state = "CONTAINED" OR 
                        r.embedding_timestamp < :report_embedding_timestamp OR (
                            r.embedding_timestamp = :report_embedding_timestamp AND
                            r.id < :report_id
                        )
                    )
            """), {
                "workspace_id": workspace_id,
                "report_id": report_id,
                "report_embedding_timestamp": embedding_timestamp
            })
            try:
                for (id, em) in results:
                    indexed_report_ids.append(id)
                    # Convert vector embedding back from bytes to tensor too
                    embeddings.append(np.frombuffer(em, dtype=np.float32))
                if len(embeddings) > 0:
                    embeddings = torch.from_numpy(np.stack(embeddings))
                else:
                    print("No candidate reports where found to detect duplicates for report ID:", report_id)
            except Exception as e2:
                print("Failed to decode embeddings from candidate reports for report ID:", report_id, e2)
        except Exception as e:
            print("Failed to fetch candidate reports:", e)
        finally:
            return indexed_report_ids, embeddings