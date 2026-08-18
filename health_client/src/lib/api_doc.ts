export async function apiFetch<T = Record<string, string>>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const response = await fetch(`http://localhost:8082${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
  });

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }

  return response.json() as Promise<T>;
}
