# Thumbnail Project

This project generates and manages thumbnails for images, optimizing them for various use cases such as websites, applications, or media libraries.

## Features

- **Image Resizing**: Generate thumbnails of specified dimensions.
- **Format Conversion**: Convert images to supported formats (e.g., JPEG, PNG, WEBP).
- **Batch Processing**: Process multiple files at once.

## Installation

1. Clone the repository:
   ```bash
   git clone https://bitbucket.lab.ii.agh.edu.pl/scm/to2024/mi-pt-1500-jelenie.git
   cd mi-pt-1500-jelenie
   ```

## Running the Application with Docker

To run the application using Docker, follow these steps:

1. **Ensure Docker and Docker Compose are installed**:
   - Install Docker: [https://docs.docker.com/get-docker/](https://docs.docker.com/get-docker/)
   - Install Docker Compose: [https://docs.docker.com/compose/install/](https://docs.docker.com/compose/install/)

2. **Pre-Build Recommendation** (Optional):
   Before building the application, it is recommended but not mandatory to navigate to the `/backend` folder and run:
   ```bash
   ./gradlew build -x test
   ```

3. **Build and run the Docker containers**:
   To run the application:
   ```bash
   docker-compose --profile default up --build
   ```

   This will:
   - Build the Docker images for the backend, frontend, and database.
   - Start the services defined in the `docker-compose.yml` file.

4. **Access the application**:
   - Frontend: Open your browser and navigate to [http://localhost:3000](http://localhost:3000)
   - Backend API: The backend will be available at [http://localhost:8080](http://localhost:8080)
   - Database: The PostgreSQL database will be exposed on `localhost:5432`.

5. **Stop the containers**:
   To stop the running containers, use:
   ```bash
   docker-compose --profile default down
   ```
   Or, to remove all containers, volumes, and networks:
   ```bash
   docker-compose --profile default down -v
   ```

6. **Optional: Run a test database for Controller testing**:
   To set up the test database, use:
   ```bash
   docker-compose --profile db_test up --build
   ```
   To remove it, use:
   ```bash
   docker-compose --profile db_test down
   ```
   Or to remove all containers, volumes, and networks:
   ```bash
   docker-compose --profile db_test down -v
   ```

## Change Log

### Milestone 1

- **[06-12-2024]** Add .gitignore
- **[07-12-2024]** Initialize project
- **[08-12-2024]** Add required Gradle dependencies
- **[09-12-2024]** Implement thumbnails service
- **[09-12-2024]** Add Docker to the project
- **[10-12-2024]** Add database
- **[10-12-2024]** Implement basic front-end and back-end communication
- **[11-12-2024]** Code simple UI
- **[12-12-2024]** Upgrade simple UI
- **[12-12-2024]** Implement database repository and models
- **[12-12-2024]** Added database initialization to docker
- **[12-12-2024]** New message types handling
- **[12-12-2024]** Init readme
- **[12-12-2024]** Original image on click fixing
- **[12-12-2024]** Final implementation
- **[12-12-2024]** Add parallel
- **[12-12-2024]** Final implementation
- **[12-12-2024]** Code refactoring before M1
- **[20-12-2024]** Refactoring and cleanup
- **[20-12-2024]** Services tests
- **[20-12-2024]** Errors handling
- **[20-12-2024]** Final refactoring
- **[20-12-2024]** Fix bugs
- **[20-12-2024]** Docker for tests
- **[20-12-2024]** Frontend polishing

## Links
- [Documentation](https://lucid.app/lucidchart/d1242d14-599a-4b54-be8e-1b0afc12c6f4/edit?viewport_loc=9882%2C-418%2C14033%2C6586%2C0_0&invitationId=inv_491730a8-bb13-4598-88fb-191e7f3f69cd)

