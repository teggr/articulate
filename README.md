# articulate

A service for converting YouTube videos into readable blog articles.

![articulate form image](/docs/Articulate-form.png)

## How it works

1. Paste a YouTube URL
2. The backend fetches the transcript from Supadata's API
3. The transcript is cleaned (filler words, `[Music]` tags, etc. removed)
4. A two-step AI pipeline (Gemini Flash via Spring AI) converts the transcript into a polished Markdown blog article
5. The API returns the title, Markdown, and rendered HTML

![articulate article](/docs/Articulate-article.png)

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
SPRING_SECURITY_USER_NAME=articulate \
SPRING_SECURITY_USER_PASSWORD=change-me \
mvn spring-boot:run
```

Or export the environment variable first:

```bash
export GEMINI_API_KEY=your-key-here
export SUPADATA_API_KEY=your-key-here
export SPRING_SECURITY_USER_NAME=articulate
export SPRING_SECURITY_USER_PASSWORD=change-me
cd backend && mvn spring-boot:run
```

Then open `http://localhost:8080/` for the public landing page.
The article generator UI is at `http://localhost:8080/generate` and requires authentication.
Spring Security will require authentication for `/generate` and API endpoints using the configured username and password.

## Browser bookmarklet (production)

Use this bookmarklet to send the current YouTube page (or a pasted YouTube URL) directly to Articulate with the URL prefilled:

```text
javascript:(()=>{const host=u=>{try{return new URL(u).hostname.toLowerCase().replace(/^www\./,'')}catch{return''}};const isYt=u=>['youtube.com','m.youtube.com','music.youtube.com','youtu.be'].includes(host(u));let y=isYt(location.href)?location.href:prompt('Paste YouTube URL');if(!y)return;if(!isYt(y)){alert('Please provide a valid YouTube URL.');return;}location.href='https://articulate.me.uk/generate?url='+encodeURIComponent(y);})();
```

For iPhone setup, see this guide on creating a share shortcut:

- [Create iPhone shortcut for sharing links](https://robintegg.com/2025/05/11/create-iphone-shortcut-for-sharing-links)

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
