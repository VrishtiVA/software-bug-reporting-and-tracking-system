import unittest
from unittest.mock import MagicMock
import sys
sys.modules["sentence_transformers"] = MagicMock()
sys.modules["torch"] = MagicMock()
from duplicate_detection_service.app.services import cleanText, cleanReport

class cleanReportTests(unittest.TestCase):
    """
    Unit test class for cleaning reports.
    You could run these in this test class using: `python -m unittest tests/clean_report_tests.py -v`
    The `-v` is for verbose mode, more detailed per-test output.
    Or run all tests using run_tests via `python run_tests.py`. 
    Note: Ensure you're in the microservice directiory.
    """

    def test_clean_report_format(self):
        response = cleanReport(
            "This is the report summary text",
            "These are the steps to reproduce the bug",
            "These are the expected results",
            "These are the actual results"
        )
        expected_keys = {"summary", "steps", "expected", "actual"}
        self.assertIsInstance(response, dict, "The returned response is not a dictionary")
        self.assertEqual(response.keys(), expected_keys, "The keys in the dictionary aren't as expected.")
        
    def test_clean_text_lower_case(self):
        self.assertEqual(
            cleanText("This is the report summary text"), 
            "this is the report summary text", 
            "Text was not lower-cased."
        )
    
    def test_clean_text_white_space(self):
        self.assertEqual(
            cleanText("This is the             report summary text"), 
            "this is the report summary text", 
            "Text didn't clear multiple spaces."
        )
        self.assertEqual(
            cleanText("This is the \treport summary text"), 
            "this is the report summary text", 
            "Text didn't clear tabs."
        )
    
    def test_clean_text_formatting_characters(self):
        self.assertEqual(
            cleanText("This is the \r\nreport summary text"), 
            "this is the report summary text", 
            "Text didn't clear formatting characters."
        )
    
    def test_clean_text_normalise_slashes(self):
        self.assertEqual(
            cleanText("In the file path \\duplicate_detection_service\\tests\\... "),
            "in the file path /duplicate_detection_service/tests/...",
            "Couldn't normalise slashes forward."
        )
    
    def test_clean_text_generalise_links(self):
        expected_response = "link: <URL>"
        self.assertEqual(cleanText("Link: https://a.com"), expected_response, "Starting with https attempt didn't match expected response.")
        self.assertEqual(cleanText("(Link: https://a.com)"), ("("+expected_response+")"), "Starting with https in brackets attempt didn't match expected response.")
        self.assertEqual(cleanText("Link: https://a.com extra"), (expected_response + " extra"), "Starting with https with extra text didn't match expected response.")
        self.assertEqual(cleanText("Link: http://a.com"), expected_response, "Starting with http attempt didn't match expected response.")
        self.assertEqual(cleanText("(Link: http://a.com)"), ("("+expected_response+")"), "Starting with http in brackets attempt didn't match expected response.")
        self.assertEqual(cleanText("Link: http://a.com extra"), (expected_response + " extra"), "Starting with http with extra text didn't match expected response.")
        self.assertEqual(cleanText("Link: www.a.com"), expected_response, "Starting with www attempt didn't match expected response.")
        self.assertEqual(cleanText("(Link: www.a.com)"), ("("+expected_response+")"), "Starting with www in brackets attempt didn't match expected response.")
        self.assertEqual(cleanText("Link: www.a.com extra"), (expected_response + " extra"), "Starting with www with extra text didn't match expected response.")
        self.assertEqual(cleanText("Link: https://"), "link: https://", "https alone didn't match expected response.")
        self.assertEqual(cleanText("Link: http://"), "link: http://", "http alone didn't match expected response.")
        self.assertEqual(cleanText("Link: www."), "link: www.", "www alone didn't match expected response.")

if __name__ == "__main__":
    unittest.main()