import subprocess
import time
import os

# Funkcja do uruchamiania komendy systemowej
def run_command(command):
    result = subprocess.run(command, shell=True, text=True, capture_output=True)
    if result.returncode != 0:
        print(f"Error: {result.stderr}")
        raise Exception(f"Command failed: {command}")
    return result.stdout

# Uruchomienie kontenera bazy danych
def start_database():
    print("Uruchamianie kontenera bazy danych...")
    run_command("docker-compose up -d db_test")

# Sprawdzanie dostępności bazy danych
def wait_for_database():
    print("Czekam na uruchomienie bazy danych...")
    while True:
        result = subprocess.run(
            "docker-compose up db_test",
            shell=True, text=True, capture_output=True
        )
        if result.returncode == 0:
            print("Baza danych jest gotowa!")
            break
        else:
            print("Baza danych nie jest jeszcze gotowa, czekam...")
            time.sleep(2)

# Uruchomienie testów Gradle
def run_tests():
    print("Uruchamianie testów Gradle...")

    current_directory = os.path.dirname(os.path.abspath(__file__))

    # Zbuduj ścieżkę do folderu backendu na podstawie lokalizacji skryptu
    backend_directory = os.path.join(current_directory, 'backend')

    # Zmień katalog roboczy na folder backendu
    os.chdir(backend_directory)

    result = subprocess.run("gradlew check", shell=True, text=True, capture_output=True)
    if result.returncode != 0:
        print(f"Testy nie powiodły się: {result.stderr}")
        raise Exception("Testy zakończone niepowodzeniem")
    else:
        print("Testy zakończone sukcesem!")

# Zatrzymanie kontenerów Docker po zakończeniu testów
def stop_database():
    print("Zatrzymywanie kontenerów Docker...")
    run_command("docker-compose down")  # Zatrzymuje kontenery

def main():
    try:
        start_database()
        # wait_for_database()
        run_tests()
    finally:
        stop_database()

if __name__ == "__main__":
    main()
