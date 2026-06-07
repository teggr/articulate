# articluate

A service for converting YouTube videos into readable blog articles.

## How it works

1. Paste a YouTube URL
2. The backend extracts the video transcript directly from YouTube
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

## Running locally

```bash
cd backend
GEMINI_API_KEY=your-key-here mvn spring-boot:run
```

Or export the environment variable first:

```bash
export GEMINI_API_KEY=your-key-here
cd backend && mvn spring-boot:run
```

Then open `http://localhost:8080/` for the web UI.

## API

### Generate article

```
POST /api/articles
Content-Type: application/json

{
  "youtubeUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
}
```

Response:

```json
{
  "title": "...",
  "markdown": "...",
  "html": "..."
}
```
