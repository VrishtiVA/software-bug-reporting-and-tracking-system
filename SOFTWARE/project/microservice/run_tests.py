import unittest
import HtmlTestRunner

# Run all tests using this script via `python run_tests.py`. 
# Note: Ensure you're in the microservice directiory.

if __name__ == "__main__":

    # Suite of all tests in tests folder
    suite = unittest.TestLoader().discover(start_dir="tests", pattern="*_tests.py")

    # Configuration to run tests and generate results in a html file
    runner = HtmlTestRunner.HTMLTestRunner(
        output="microservice_unit_test_results",
        report_name="microservice_unit_test_results",
        report_title="Microservice Unit Test Results",
        combine_reports=True,
        verbosity=2
    )
    
    # Run test suite
    runner.run(suite)