FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy your application files into the container
COPY . /app

# Expose the port for peer-to-peer UDP discovery
EXPOSE 8888
EXPOSE 9999

# Command to run your application
CMD ["java", "-cp", "target/classes", "com.github.fevzibabaoglu.App"]
