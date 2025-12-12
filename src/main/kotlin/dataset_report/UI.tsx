import React, { useState, useRef, useEffect } from "react";

type UploadedFile = {
    id: string;
    filename: string;
    path?: string;
    role: "input" | "output" | "other";
};

type ChatMessage = {
    id: string;
    role: "user" | "assistant" | "system";
    text: string;
    time?: string;
};

export default function DatasetChatUI() {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [text, setText] = useState("");
    const [files, setFiles] = useState<UploadedFile[]>([]);
    const [dragOver, setDragOver] = useState(false);
    const [uploading, setUploading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const fileInputRef = useRef<HTMLInputElement | null>(null);
    const messagesRef = useRef<HTMLDivElement | null>(null);

    useEffect(() => {
        // initial system message (optional)
        setMessages([{
            id: crypto.randomUUID(),
            role: "system",
            text: "I am your dataset assistant. You can upload files and chat with me. Use the file roles to indicate input/output.",
            time: new Date().toISOString()
        }]);
    }, []);

    useEffect(() => {
        // scroll to bottom when messages change
        if (messagesRef.current) {
            messagesRef.current.scrollTop = messagesRef.current.scrollHeight;
        }
    }, [messages]);

    function onFilesSelected(evt: React.ChangeEvent<HTMLInputElement>) {
        const list = evt.target.files;
        if (!list) return;
        handleLocalFiles(Array.from(list));
        // reset input so same file can be re-selected later
        evt.currentTarget.value = "";
    }

    function handleLocalFiles(selected: File[]) {
        // optimistic local append (not uploaded yet), role defaults to input
        const local = selected.map(f => ({ id: crypto.randomUUID(), filename: f.name, role: "input" as const }))
        setFiles(prev => [...prev, ...local]);
        // auto-upload
        uploadFiles(selected, local.map(l => l.id));
    }

    async function uploadFiles(fileList: File[], localIds: string[]) {
        setUploading(true);
        setError(null);
        try {
            for (let i = 0; i < fileList.length; i++) {
                const file = fileList[i];
                const localId = localIds[i];
                const fd = new FormData();
                fd.append("file", file);
                // Modify URL to match your backend route
                const res = await fetch("/api/upload", { method: "POST", body: fd });
                if (!res.ok) throw new Error(`Upload failed: ${res.status}`);
                const payload = await res.json();
                // payload expected: { id, filename, path }
                setFiles(prev => prev.map(p => p.id === localId ? { ...p, id: payload.id ?? p.id, path: payload.path ?? p.path, filename: payload.filename ?? p.filename } : p));
            }
        } catch (e: any) {
            console.error(e);
            setError(e.message || "Upload error");
        } finally {
            setUploading(false);
        }
    }

    function onDrop(e: React.DragEvent) {
        e.preventDefault();
        setDragOver(false);
        const list = Array.from(e.dataTransfer.files || []);
        if (list.length) handleLocalFiles(list);
    }

    function onDragOver(e: React.DragEvent) {
        e.preventDefault();
        setDragOver(true);
    }

    function onDragLeave() {
        setDragOver(false);
    }

    function removeFile(id: string) {
        setFiles(prev => prev.filter(f => f.id !== id));
    }

    function updateFileRole(id: string, role: UploadedFile["role"]) {
        setFiles(prev => prev.map(f => f.id === id ? { ...f, role } : f));
    }

    async function sendMessage() {
        if (!text.trim()) return;
        const userMsg: ChatMessage = { id: crypto.randomUUID(), role: "user", text: text.trim(), time: new Date().toISOString() };
        setMessages(prev => [...prev, userMsg]);
        setText("");

        // Prepare payload: include file ids and roles
        const payload = {
            message: userMsg.text,
            files: files.map(f => ({ id: f.id, filename: f.filename, role: f.role }))
        };

        try {
            const res = await fetch("/api/chat", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            if (!res.ok) throw new Error(`Chat request failed: ${res.status}`);
            const data = await res.json();
            // expect { reply: string }
            const assistantMsg: ChatMessage = { id: crypto.randomUUID(), role: "assistant", text: data.reply ?? "(no reply)", time: new Date().toISOString() };
            setMessages(prev => [...prev, assistantMsg]);
        } catch (e: any) {
            const errMsg: ChatMessage = { id: crypto.randomUUID(), role: "assistant", text: `Error: ${e.message || 'request failed'}`, time: new Date().toISOString() };
            setMessages(prev => [...prev, errMsg]);
        }
    }

    async function handleEnter(e: React.KeyboardEvent<HTMLTextAreaElement>) {
        if ((e.ctrlKey || e.metaKey) && !e.shiftKey) {
            e.preventDefault();
            await sendMessage();
        }
    }

    return (
        <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
        <div className="max-w-4xl w-full bg-white shadow-md rounded-2xl overflow-hidden flex flex-col">

            {/* Header */}
            <div className="px-6 py-4 border-b">
    <h1 className="text-xl font-semibold">Dataset Chat UI</h1>
    <p className="text-sm text-slate-500">Chat with the dataset assistant. Upload files to reference them in chat.</p>
    </div>

    <div className="flex gap-4 p-6">
        {/* Left: chat area */}
        <div className="flex-1 flex flex-col">
    <div ref={messagesRef} className="flex-1 overflow-auto p-2 space-y-3 bg-slate-50 rounded-md" style={{ maxHeight: '60vh' }}>
    {messages.map(m => (
        <div key={m.id} className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}>
        <div className={`${m.role === 'user' ? 'bg-indigo-600 text-white' : m.role === 'system' ? 'bg-slate-200 text-slate-800' : 'bg-slate-100 text-slate-900'} px-4 py-2 rounded-lg max-w-[75%] whitespace-pre-wrap`}>
        <div className="text-sm">{m.text}</div>
            <div className="text-[10px] opacity-60 mt-1 text-right">{new Date(m.time || '').toLocaleString()}</div>
        </div>
        </div>
    ))}
    </div>

    <div className="mt-3">
    <textarea
        value={text}
    onChange={e => setText(e.target.value)}
    onKeyDown={handleEnter}
    rows={3}
    placeholder="Type your message here — press Ctrl/Cmd+Enter to send"
    className="w-full resize-none rounded-md border p-2"
    />
    <div className="flex items-center justify-between mt-2">
    <div className="flex gap-2">
    <button onClick={() => fileInputRef.current?.click()} className="px-3 py-1 rounded bg-slate-100 border">Attach</button>
        <input ref={fileInputRef} type="file" className="hidden" multiple onChange={onFilesSelected} />
    <div className="text-sm text-slate-500">{uploading ? 'Uploading...' : ''}{error ? ` Error: ${error}` : ''}</div>
    </div>
    <div className="flex gap-2">
    <button onClick={sendMessage} className="px-4 py-2 rounded bg-indigo-600 text-white">Send</button>
        </div>
        </div>
        </div>
        </div>

    {/* Right: files & uploader */}
    <div className="w-80">
    <div className={`border-2 rounded-md p-3 transition ${dragOver ? 'border-indigo-400 bg-indigo-50' : 'border-dashed border-slate-200 bg-white'}`} onDrop={onDrop} onDragOver={onDragOver} onDragLeave={onDragLeave}>
    <div className="text-sm font-medium">Upload files</div>
    <div className="text-xs text-slate-500 mt-1">Drag & drop files here or click Attach.</div>
    <div className="mt-3 space-y-2">
        {files.length === 0 && <div className="text-xs text-slate-400">No files selected</div>}
    {files.map(f => (
        <div key={f.id} className="flex items-center justify-between bg-slate-50 p-2 rounded">
    <div className="flex-1">
    <div className="text-sm font-medium truncate">{f.filename}</div>
        <div className="text-xs text-slate-500">{f.path ?? 'Not yet uploaded'}</div>
        </div>
        <div className="flex items-center gap-2">
    <select value={f.role} onChange={e => updateFileRole(f.id, e.target.value as UploadedFile['role'])} className="text-xs border rounded px-1">
    <option value="input">input</option>
        <option value="output">output</option>
        <option value="other">other</option>
        </select>
        <button onClick={() => removeFile(f.id)} className="text-xs px-2">Remove</button>
        </div>
        </div>
    ))}
    </div>
    </div>

    <div className="mt-4 text-xs text-slate-500">
        Tip: assign role "input" to datasets you want the agent to read, and "output" to files where the agent should save results.
    </div>
    </div>
    </div>

    <div className="px-6 py-3 border-t text-xs text-slate-500">
        Backend endpoints expected: <strong>/api/upload</strong> and <strong>/api/chat</strong>. See top comments in file for details.
    </div>
    </div>
    </div>
);
}
