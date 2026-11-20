import { useEffect, useRef, useState } from 'react';
import { Alert, Button, ProgressBar } from 'react-bootstrap';

const DURATION_MS = 5000;
const SAMPLE_AT_MS = [400, 1700, 3000, 4600];
const MAX_WIDTH = 1600;

interface ScreenRecorderProps {
    /** Receives a few sampled JPEG frames. The video itself is never stored or uploaded. */
    onFrames: (frames: File[]) => void;
    disabled?: boolean;
}

export function screenCaptureSupported(): boolean {
    return typeof navigator !== 'undefined' && !!navigator.mediaDevices?.getDisplayMedia;
}

function grabFrame(video: HTMLVideoElement, index: number): Promise<File | null> {
    if (!video.videoWidth) return Promise.resolve(null);
    const scale = Math.min(1, MAX_WIDTH / video.videoWidth);
    const canvas = document.createElement('canvas');
    canvas.width = Math.round(video.videoWidth * scale);
    canvas.height = Math.round(video.videoHeight * scale);
    canvas.getContext('2d')?.drawImage(video, 0, 0, canvas.width, canvas.height);
    return new Promise((resolve) =>
        canvas.toBlob((blob) => resolve(blob ? new File([blob], `frame-${index + 1}.jpg`, { type: 'image/jpeg' }) : null),
            'image/jpeg', 0.82));
}

/**
 * Five-second screen capture with explicit browser permission, a visible status and a Stop button.
 * Frames are sampled from the live stream with a canvas; no video file is created.
 */
export default function ScreenRecorder({ onFrames, disabled }: ScreenRecorderProps) {
    const [recording, setRecording] = useState(false);
    const [elapsed, setElapsed] = useState(0);
    const [error, setError] = useState<string | null>(null);
    const streamRef = useRef<MediaStream | null>(null);
    const stopRef = useRef<(() => void) | null>(null);

    useEffect(() => () => streamRef.current?.getTracks().forEach((t) => t.stop()), []);

    if (!screenCaptureSupported()) {
        return (
            <Alert variant="secondary" className="small mb-0">
                Screen recording isn't supported in this browser. Please upload a screenshot instead.
            </Alert>
        );
    }

    const start = async () => {
        setError(null);
        let stream: MediaStream;
        try {
            stream = await navigator.mediaDevices.getDisplayMedia({ video: { frameRate: 5 }, audio: false });
        } catch (err) {
            const name = (err as DOMException)?.name;
            setError(name === 'NotAllowedError'
                ? 'Screen capture was cancelled or blocked. You can upload a screenshot instead.'
                : 'Screen capture could not start. You can upload a screenshot instead.');
            return;
        }
        streamRef.current = stream;
        const video = document.createElement('video');
        video.muted = true;
        video.srcObject = stream;
        await video.play().catch(() => undefined);

        setRecording(true);
        setElapsed(0);
        const frames: File[] = [];
        const startedAt = performance.now();
        const timers: number[] = [];
        let finished = false;

        const finish = async () => {
            if (finished) return;
            finished = true;
            timers.forEach((t) => window.clearTimeout(t));
            window.clearInterval(ticker);
            if (frames.length === 0) {
                const last = await grabFrame(video, 0);
                if (last) frames.push(last);
            }
            stream.getTracks().forEach((t) => t.stop());
            streamRef.current = null;
            video.srcObject = null;
            setRecording(false);
            if (frames.length) onFrames(frames);
            else setError('No frames could be captured. Please upload a screenshot instead.');
        };
        stopRef.current = finish;

        const ticker = window.setInterval(() => setElapsed(Math.min(DURATION_MS, performance.now() - startedAt)), 100);
        SAMPLE_AT_MS.forEach((at, i) => timers.push(window.setTimeout(async () => {
            const frame = await grabFrame(video, i);
            if (frame) frames.push(frame);
        }, at)));
        timers.push(window.setTimeout(finish, DURATION_MS + 150));
        stream.getVideoTracks()[0]?.addEventListener('ended', finish);
    };

    return (
        <div>
            {recording ? (
                <div className="tiqvision-recording" role="status" aria-live="polite">
                    <div className="d-flex align-items-center justify-content-between mb-2">
                        <span><span className="tiqvision-rec-dot" aria-hidden="true" /> Recording your screen… {(elapsed / 1000).toFixed(1)}s / 5s</span>
                        <Button size="sm" variant="danger" onClick={() => stopRef.current?.()}>■ Stop</Button>
                    </div>
                    <ProgressBar now={(elapsed / DURATION_MS) * 100} variant="danger" aria-label="Recording progress" />
                </div>
            ) : (
                <Button variant="outline-light" disabled={disabled} onClick={start}>
                    🎥 Record 5 seconds of my screen
                </Button>
            )}
            <div className="small text-muted mt-2">
                Your browser will ask which screen or window to share. Only a few still frames are sent for analysis;
                the recording itself is not saved.
            </div>
            {error && <Alert variant="warning" className="py-2 mt-2 mb-0 small">{error}</Alert>}
        </div>
    );
}
