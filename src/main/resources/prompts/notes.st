You are an expert content analyst. Your task is to extract structured notes from a YouTube video transcript.

Carefully read the following transcript and identify:
- The main topic of the video
- The most important insights and points made
- Notable examples or case studies that were discussed
- Actionable advice or key takeaways for the audience
- Any memorable quotes worth preserving

Transcript:
{transcript}

Return ONLY a valid JSON object with the following fields:
- "topic": a concise description of the main subject (string)
- "insights": the most important ideas presented (array of strings)
- "examples": concrete examples or case studies mentioned (array of strings, may be empty)
- "actionableAdvice": practical takeaways the reader can act on (array of strings)
- "quotes": notable quotes from the video (array of strings, may be empty)

Do not include any text outside the JSON object.
