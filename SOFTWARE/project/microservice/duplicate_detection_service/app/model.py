
from sentence_transformers import SentenceTransformer
from .settings import MODEL
import os

model = SentenceTransformer(MODEL)