export class ApiError extends Error {
  constructor(message: string, public readonly status: number) { super(message); this.name = "ApiError"; }
}

export async function apiFetch<T = Record<string, string>>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  try{
    const isFormdata = options.body instanceof FormData;
    const baseUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8087';

    const headers: HeadersInit = {
      ...(isFormdata ? {} : { "Content-Type": "application/json" }),
      ...(typeof window !== "undefined" && localStorage.getItem("token")
        ? { Authorization: `Bearer ${localStorage.getItem("token")}` } : {}),
      ...options.headers,
    };
    const response = await fetch(`${baseUrl}${path}`,
     {
        ...options,
        credentials: "include",
        headers,
      });

  if (!response.ok) {
    const detail = await response.json().catch(() => ({}));
    throw new ApiError(detail.error || detail.message || `Request failed: ${response.status}`, response.status);
  }

  return response.json() as Promise<T>;
    }
    catch(error){
      console.error("API error:",error);
      throw error;
    }
  }
 


export async function reportFileUrl(id: string): Promise<string> {
  const baseUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8087";
  const response = await fetch(`${baseUrl}/api/reports/${encodeURIComponent(id)}/pdf`, {
    headers: { Authorization: `Bearer ${localStorage.getItem("token") || ""}` },
    credentials: "include",
    cache: "no-store",
  });
  if (!response.ok) {
    const detail = await response.json().catch(() => ({}));
    throw new Error(detail.error || `Unable to open report (${response.status})`);
  }
  return URL.createObjectURL(await response.blob());
}
