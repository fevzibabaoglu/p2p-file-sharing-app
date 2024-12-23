# Variables
IMAGE_NAME = p2p_app
COMPOSE_FILE = docker-compose.yml

# Build the Docker image
build:
	docker build -t $(IMAGE_NAME) .

# Start the services using Docker Compose
up: build
	docker-compose -f $(COMPOSE_FILE) up

# Stop the services
down:
	docker-compose -f $(COMPOSE_FILE) down

# Rebuild the image and restart services
rebuild: down build up

# Clean up unused resources
clean:
	docker system prune -f
