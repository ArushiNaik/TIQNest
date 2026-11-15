import { useEffect, useMemo, useRef, useState, type DragEvent } from 'react';
import { Button } from 'react-bootstrap';

export const ACCEPTED_TYPES = ['image/png', 'image/jpeg', 'image/webp'];
export const MAX_FILES = 4;
export const MAX_FILE_BYTES = 3.5 * 1024 * 1024;

interface ScreenshotDropzoneProps {
    files: File[];
    onChange: (files: File[]) => void;
    disabled?: boolean;
}

/** File picker + drag and drop + paste. The backend re-validates every file from its bytes. */
export default function ScreenshotDropzone({ files, onChange, disabled }: ScreenshotDropzoneProps) {
    const inputRef = useRef<HTMLInputElement>(null);
    const [dragging, setDragging] = useState(false);
    const [problem, setProblem] = useState<string | null>(null);
    const previews = useMemo(() => files.map((f) => ({ file: f, url: URL.createObjectURL(f) })), [files]);

    useEffect(() => () => previews.forEach((p) => URL.revokeObjectURL(p.url)), [previews]);

    const add = (incoming: File[]) => {
        setProblem(null);
        const accepted: File[] = [];
        for (const f of incoming) {
            if (!ACCEPTED_TYPES.includes(f.type)) {
                setProblem(`"${f.name}" is not a PNG, JPEG or WebP image.`);
                continue;
            }
            if (f.size > MAX_FILE_BYTES) {
                setProblem(`"${f.name}" is larger than 3.5 MB.`);
                continue;
            }
            accepted.push(f);
        }
        const next = [...files, ...accepted].slice(0, MAX_FILES);
        if (files.length + accepted.length > MAX_FILES) setProblem(`You can attach up to ${MAX_FILES} images.`);
        onChange(next);
    };

    useEffect(() => {
        if (disabled) return;
        const onPaste = (e: ClipboardEvent) => {
            const pasted = Array.from(e.clipboardData?.files ?? []).filter((f) => f.type.startsWith('image/'));
            if (pasted.length) add(pasted.map((f, i) => new File([f], f.name || `pasted-${i + 1}.png`, { type: f.type })));
        };
        window.addEventListener('paste', onPaste);
        return () => window.removeEventListener('paste', onPaste);
    });

    const onDrop = (e: DragEvent) => {
        e.preventDefault();
        setDragging(false);
        if (!disabled) add(Array.from(e.dataTransfer.files));
    };

    return (
        <div>
            <div
                className={`tiqvision-drop ${dragging ? 'dragging' : ''} ${disabled ? 'disabled' : ''}`}
                onDragOver={(e) => {
                    e.preventDefault();
                    if (!disabled) setDragging(true);
                }}
                onDragLeave={() => setDragging(false)}
                onDrop={onDrop}
            >
                <div className="display-6 mb-1" aria-hidden="true">🖼️</div>
                <div className="fw-semibold">Drop a screenshot of the error here</div>
                <div className="small text-muted mb-2">or paste it with Ctrl+V · PNG, JPEG or WebP · up to 4 images, 3.5 MB each</div>
                <input
                    ref={inputRef}
                    id="tiqvision-file"
                    type="file"
                    accept={ACCEPTED_TYPES.join(',')}
                    multiple
                    className="visually-hidden"
                    disabled={disabled}
                    onChange={(e) => {
                        add(Array.from(e.target.files ?? []));
                        e.target.value = '';
                    }}
                />
                <Button variant="outline-light" size="sm" disabled={disabled} onClick={() => inputRef.current?.click()}>
                    Choose screenshot…
                </Button>
            </div>
            {problem && <div className="small text-warning mt-2" role="alert">{problem}</div>}
            {previews.length > 0 && (
                <div className="tiqvision-previews mt-3">
                    {previews.map(({ file, url }, i) => (
                        <figure key={url} className="tiqvision-preview">
                            <img src={url} alt={`Attached screenshot ${i + 1}: ${file.name}`} />
                            <figcaption className="small text-truncate">{file.name}</figcaption>
                            <button
                                type="button"
                                className="tiqvision-remove"
                                aria-label={`Remove ${file.name}`}
                                disabled={disabled}
                                onClick={() => onChange(files.filter((_, j) => j !== i))}
                            >
                                ×
                            </button>
                        </figure>
                    ))}
                </div>
            )}
        </div>
    );
}
