# articluate

A service for converting YouTube videos into readable blog articles.

## How it works

1. Paste a YouTube URL
2. The backend fetches the transcript from Supadata's API
3. The transcript is cleaned (filler words, `[Music]` tags, etc. removed)
4. A two-step AI pipeline (Gemini Flash via Spring AI) converts the transcript into a polished Markdown blog article
5. The API returns the title, Markdown, and rendered HTML

## Tech stack

| Layer    | Technology                                    |
|----------|-----------------------------------------------|
| Backend  | Java 21, Spring Boot 3.5, Spring AI 1.0       |
| AI model | Gemini 2.5 Flash (via OpenAI-compatible API)  |

## Prerequisites

- Java 21+
- Maven 3.9+
- A [Google AI Studio](https://aistudio.google.com/) API key (free tier available)
- A [Supadata](https://supadata.ai/) API key for transcript retrieval

## Running locally

```bash
cd backend
GEMINI_API_KEY=your-key-here \
SUPADATA_API_KEY=your-key-here \
SPRING_SECURITY_USER_NAME=articluate \
SPRING_SECURITY_USER_PASSWORD=change-me \
mvn spring-boot:run
```

Or export the environment variable first:

```bash
export GEMINI_API_KEY=your-key-here
export SUPADATA_API_KEY=your-key-here
export SPRING_SECURITY_USER_NAME=articluate
export SPRING_SECURITY_USER_PASSWORD=change-me
cd backend && mvn spring-boot:run
```

Then open `http://localhost:8080/` for the public landing page.
The article generator UI is at `http://localhost:8080/generate` and requires authentication.
Spring Security will require authentication for `/generate` and API endpoints using the configured username and password.

## Docker + deploy4j release flow

This repository now follows the same deployment approach as yorkshire-golf:

1. Build and package the app (also builds Docker image via Maven plugin)

```shell
mvn -B package --file pom.xml
```

2. Push the release Docker image

```shell
mvn -B docker:push --file pom.xml
```

3. Deploy a specific version with deploy4j

```shell
deploy4j deploy --version=0.0.1
```

Configuration files:

- Docker image build: `Dockerfile` + `pom.xml` (`io.fabric8:docker-maven-plugin`)
- deploy4j runtime config: `config/deploy.yml`
- automated release + deploy pipeline: `.github/workflows/release-and-deploy.yml`

Required GitHub secrets for the workflow:

- `DOCKER_USERNAME`
- `DOCKER_PASSWORD`
- `PRIVATE_KEY`
- `PRIVATE_KEY_PASSPHRASE`
- `GEMINI_API_KEY`
- `SUPADATA_API_KEY`
- `SPRING_SECURITY_USER_NAME`
- `SPRING_SECURITY_USER_PASSWORD`
