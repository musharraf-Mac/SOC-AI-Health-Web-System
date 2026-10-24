export async function apiFetch<T = Record<string, string>>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  try{
    const isFormdata = options.body instanceof FormData;
    const baseUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8087';

    const headers: HeadersInit = {
      ...(isFormdata ? {} : { "Content-Type": "application/json" }),
      ...options.headers,
    };
    const response = await fetch(`${baseUrl}${path}`,
     {
        ...options,
        credentials: "include",
        headers,
      });

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }

  return response.json() as Promise<T>;
    }
    catch(error){
      console.error("API error:",error);
      throw error;
    }
  }
 

