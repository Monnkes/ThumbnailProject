import subprocess
import time
import os

# Function to execute a system command
def run_command(command):
    result = subprocess.run(command, shell=True, text=True, capture_output=True)
    if result.returncode != 0:
        print(f"Error: {result.stderr}")
        raise Exception(f"Command failed: {command}")
    return result.stdout

# Start the database container
def start_database():
    print("Starting the database container...")
    run_command("docker-compose --profile db_test up --build")

# Check database availability
def wait_for_database():
    print("Waiting for the database to become available...")
    while True:
        result = subprocess.run(
            "docker-compose --profile db_test up --build",
            shell=True, text=True, capture_output=True
        )
        if result.returncode == 0:
            print("The database is ready!")
            break
        else:
            print("The database is not ready yet, waiting...")
            time.sleep(2)

# Run Gradle tests and print failed tests
def run_tests():
    print("Running Gradle tests...")

    current_directory = os.path.dirname(os.path.abspath(__file__))

    # Build the path to the backend folder based on the script location
    backend_directory = os.path.join(current_directory, 'backend')

    # Change the working directory to the backend folder
    os.chdir(backend_directory)

    # Run Gradle with --continue option to allow further tests even if some fail
    result = subprocess.run("gradlew check --continue", shell=True, text=True, capture_output=True)

    # Capture the output and check if there were failed tests
    if result.returncode != 0:
        print("\033[91mTests did not pass. Here are the failed tests:\n\033[0m", result.stderr)
    else:
        print("\033[92mTests passed successfully!\033[0m")

# Stop Docker containers after tests
def stop_database():
    print("Stopping Docker containers...")
    run_command("docker-compose --profile db_test down -v")  # Stops containers

def main():
    try:
        start_database()
#         wait_for_database()
        run_tests()
    finally:
        stop_database()

if __name__ == "__main__":
    main()
