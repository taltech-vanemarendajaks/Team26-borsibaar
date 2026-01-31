import { ApiError, NotAuthenticatedError, ForbiddenError } from '../../../utils/apiError';


export async function fetchWithAuth(input: RequestInfo, init?: RequestInit) {
  const response = await fetch(input, init);

  if (!response.ok) {
    if (response.status === 401) {
      throw new NotAuthenticatedError(); //Not logged in
    } else if (response.status === 403) {
      throw new ForbiddenError(); //logged in but forbidden
    } else {
      const text = await response.text();
      throw new ApiError(text || 'Unknown error', response.status); //other
    }
  }

  return response.json();
}
