
# Available strategies
EMBEDDING_STRATEGIES = ["FREE_TEXT", "WEIGHTED"]

# ----- Selected model ------------------------------
# Analysis with the models was completed to select a appropriate model, field weights, and thresholds
# As discussed in the dissertation.

# MODEL = "all-MiniLM-L6-v2" # This runs faster but with slightly less accuracy.
MODEL = "all-mpnet-base-v2" # This runs slower but with slightly higher accuracy.

# ----- Selected embedding strategy -----------------
EMBEDDING_STRATEGY = EMBEDDING_STRATEGIES[1]

# Selected weights for weighted embedding strategy
WEIGHTS = {
    "summary": 0.5,
    "actual": 0.25,
    "expected": 0.15,
    "steps": 0.10
}

# ----- Thresholds & Result Count -------------------
UPPER_SIMILARITY_THESHOLD = 0.9
LOWER_SIMILARITY_THESHOLD = 0.70
TOP_K_VALUE = 5