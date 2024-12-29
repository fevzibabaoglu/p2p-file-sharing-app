FROM openjdk:17-jdk-slim

# Install X11 tools and dependencies
RUN apt-get update && apt-get install -y \
    x11-apps libxext6 libxrender1 libxtst6 libxi6 \
    && rm -rf /var/lib/apt/lists/*

# Set the working directory inside the container
WORKDIR /app

# Copy your application files into the container
COPY . /app

# Command to run your application
CMD ["java", "-cp", "target/classes", "com.github.fevzibabaoglu.App", "-docker"]
