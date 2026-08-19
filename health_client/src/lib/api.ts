export async function apiFetch<T = Record<string, string>>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  try {
    const response = await fetch(`http://localhost:8087${path}`, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...options.headers,
      },
    });

    // ⭐ Parse response body
    const responseData = await response.json();

    if (!response.ok) {
      // ⭐ Throw the actual error message from backend
      throw new Error(responseData.error || `Request failed: ${response.status}`);
    }

    return responseData as T;
  } catch (error) {
    console.error("API Error:", error);
    throw error;
  }
}