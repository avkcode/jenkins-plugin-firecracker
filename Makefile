# Build the plugin package (skip tests)
build:
	mvn clean package -DskipTests

# Quick run - Skip tests and start Jenkins
quick-run: build
	docker-compose down
	docker-compose up -d --build
	@echo "Jenkins is running at http://localhost:8080"

# Run tests
test:
	mvn test

# Start Jenkins in Docker with the plugin installed
docker-run: build
	docker-compose down
	docker-compose up -d --build
	@echo "Jenkins is running at http://localhost:8080"

# Stop Jenkins container
docker-stop:
	docker-compose down

# Clean build artifacts
clean:
	mvn clean
	rm -f *.hpi *.jpi

# Update dependency versions
update-deps:
	mvn versions:use-latest-versions

# Run Jenkins with plugin mounted (for development)
run-jenkins: build
	docker run -it --rm -p 8080:8080 -p 50000:50000 \
		-v $(shell pwd)/target/*.hpi:/usr/share/jenkins/ref/plugins/ \
		jenkins/jenkins:lts

.PHONY: build test docker-run docker-stop clean update-deps run-jenkins
