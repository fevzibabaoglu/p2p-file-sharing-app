# Variables
IMAGE_NAME = p2p_app
COMPOSE_FILE = docker-compose.yml

# Allow local Docker containers to use X server
x11_setup:
	xhost +local:docker

# Build the Docker image
build:
	docker build -t $(IMAGE_NAME) .

# Start the services using Docker Compose
up: x11_setup build
	docker-compose -f $(COMPOSE_FILE) up

# Stop the services
down:
	docker-compose -f $(COMPOSE_FILE) down

# Rebuild the image and restart services
rebuild: down build up

# Clean up unused resources
clean:
	docker system prune -f
