from pydantic import BaseModel, Field

# Note: Python int is safe with long.

# ---------- Requests ----------

class NewReportRequestBody(BaseModel):
    report_id: int
    summary: str
    steps: str
    expected: str
    actual: str

class ReportRequestBody(BaseModel):
    workspace_id: int
    report_id: int

# ---------- Responses ----------

class ReportEmbeddingResponseBody(BaseModel):
    report_id: int
    vector_embedding: str # To hold a base64 encoded string

class DuplicateSuggestionResponseBody(BaseModel):
    to_report_id: int
    similarity_order: int
    similarity_score: float
    is_confident: bool

class ReportDuplicateDetectionResponseBody(BaseModel):
    from_report_id: int
    duplicate_suggestions: list[DuplicateSuggestionResponseBody] = Field(default_factory=list)