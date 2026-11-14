async function parseResponse<T>(response: Response, fallbackMessage: string): Promise<T> {
    const resultText = await response.text();
    if (!response.ok) {
        throw new Error(resultText || fallbackMessage);
    }
    if (!resultText) {
        return undefined as T;
    }
    try {
        return JSON.parse(resultText) as T;
    } catch {
        return resultText as T;
    }
}

export async function getData<T>(apiUrl: string): Promise<T> {
    const response = await fetch(apiUrl, { method: 'GET' });
    return parseResponse<T>(response, 'Failed to load data');
}

export async function postData<T>(apiUrl: string, newData: unknown): Promise<T> {
    const response = await fetch(apiUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(newData),
    });
    return parseResponse<T>(response, 'Failed to save');
}

export async function putData<T>(apiUrl: string, updatedData: unknown): Promise<T> {
    const response = await fetch(apiUrl, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(updatedData),
    });
    return parseResponse<T>(response, 'Failed to update');
}

export async function patchData<T>(apiUrl: string, changes: unknown): Promise<T> {
    const response = await fetch(apiUrl, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(changes),
    });
    return parseResponse<T>(response, 'Failed to update');
}

export async function deleteData(apiUrl: string): Promise<void> {
    const response = await fetch(apiUrl, { method: 'DELETE' });
    await parseResponse<void>(response, `Failed to delete (status ${response.status})`);
}
