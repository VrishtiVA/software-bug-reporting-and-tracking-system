from .settings import LOWER_SIMILARITY_THESHOLD, UPPER_SIMILARITY_THESHOLD, EMBEDDING_STRATEGY, WEIGHTS, TOP_K_VALUE
from .model import model
from sentence_transformers import util
import numpy as np
import torch
import re

def cleanText(text):
    """
    Function to perform text pre-processing on some text. This involves:
    - Lower casing
    - Careful text removal
    - Minor generalisation
    - Tokenisation and beyond unnecessary as handled by the model.

    :param text: The text to clean.
    """
    # Lower casing
    text = text.lower()
    # Replacing any white space with simple space.
    text = re.sub(r"\s+", " ", text).strip()
    # Removing any other formating characters
    text = re.sub(r"[\r\n\t]", " ", text)
    # Normalising slashes into the same direction
    text = text.replace("\\", "/")
    # Generalising links (starting either http or www, matched to 1 or more characters not in []).
    text = re.sub(r"https?://[^\s)\]\}}]+|www\.[^\s)\]\}}]+", "<URL>", text)

    return text

def cleanReport(summary: str, steps: str, expected: str, actual: str) -> dict:
    """
    Function to perform text pre-processing on incoming report data.
    
    :param summary: The report summary text
    :type summary: str
    :param steps: The report steps to reproduce text
    :type steps: str
    :param expected: The report expected result text
    :type expected: str
    :param actual: The report actual results text
    :type actual: str
    :return: The cleaned report as a dictionary.
    :rtype: dict
    """
    return {
        "summary": cleanText(summary),
        "steps": cleanText(steps),
        "expected": cleanText(expected),
        "actual": cleanText(actual) 
    }

def computeVector(report_data: dict) -> torch.Tensor:
    """
    Function to compute the vector for a bug report. 
    The vector for a bug report only has to be computed once, since the bug report itself doesn't change.

    :param report_data: Description
    :type report_data: dict
    :return: The report embedding
    :rtype: torch.Tensor
    """ 
    embedding = None
    if EMBEDDING_STRATEGY == "WEIGHTED":
        embedding = (
            WEIGHTS["summary"] * model.encode(report_data["summary"]) +
            WEIGHTS["steps"] * model.encode(report_data["steps"]) +
            WEIGHTS["expected"] * model.encode(report_data["expected"]) +
            WEIGHTS["actual"] * model.encode(report_data["actual"])
        )
        return embedding
    else: 
        # Default is free-text embedding strategy
        embedding = torch.Tensor(model.encode(
            "summary: " + report_data["summary"] +
            " steps to reproduce: " + report_data["steps"] + 
            " expected results: " + report_data["expected"] + 
            " actual results: " + report_data["actual"]
        ))
        return embedding

def detectDuplicates(
    report_embedding: torch.Tensor, 
    candidate_report_embeddings: torch.Tensor, 
    indexed_candidate_report_ids: list
) -> list[tuple[int, float, bool]]:
    """
    Function to detect and suggest duplicate bug reports with their similarity scores.
    
    :param report_embedding: The vector embedding of the current report 
    :type report_embedding: torch.Tensor
    :param candidate_report_embeddings: The candidate report vector embeddings.
    :type candidate_report_embeddings: torch.Tensor
    :param indexed_candidate_report_ids: The list of candidate report IDs, where the index mapping is useful.
    :type indexed_candidate_report_ids: list
    :return: 
        Duplicate report suggestions as a list of suggestions, where 
        each suggestion is a tuple containing 
            the suggested report ID, 
            the similarity score between this report and the current report, and
            if the suggestion is confident or not
    :rtype: list[tuple[int, float, bool]]
    """
    # Calculate similarity scores using cosine similarity.
    similarity_scores = util.cos_sim(report_embedding, candidate_report_embeddings)
    
    # Finding minimum k here to avoid out of bounds error.
    top_k_value = min(TOP_K_VALUE, similarity_scores.size(1)) # Horizontal direction (dimension)
    # Find the top k reports ordered by the highest to lowest cosine similarity scores.
    top_k_similarity_scores, top_k_similarity_index = torch.topk(similarity_scores, k=top_k_value)

    # Filter for suggestions over threshold.
    detection_results = []
    for score, index in zip(top_k_similarity_scores[0], top_k_similarity_index[0]):
        if score.item() >= LOWER_SIMILARITY_THESHOLD:
            detection_results.append((
                indexed_candidate_report_ids[index.item()],
                score.item(),
                score.item() >= UPPER_SIMILARITY_THESHOLD
            ))

    return detection_results