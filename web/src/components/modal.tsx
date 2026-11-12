'use client';

import { useEffect, useRef, type ReactNode } from 'react';

type ClosedBy = 'any' | 'closerequest' | 'none';

type ModalProps = {
  open: boolean;
  onClose: () => void;
  /** id of the element that names the dialog (usually its heading) */
  titleId: string;
  children: ReactNode;
  /**
   * How the dialog can be dismissed:
   *  - `any`         light dismiss (click outside) + Esc
   *  - `closerequest` Esc / back gesture only (default)
   *  - `none`        only the developer closes it
   */
  closedby?: ClosedBy;
  className?: string;
};

/**
 * Modal built on the native <dialog> element: the browser provides the
 * top-layer rendering, the inert page behind it, focus containment, the
 * Esc / back-gesture dismissal and the focus return to the invoker.
 */
export default function Modal({
  open,
  onClose,
  titleId,
  children,
  closedby = 'closerequest',
  className = '',
}: ModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const onCloseRef = useRef(onClose);
  onCloseRef.current = onClose;

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    if (open) {
      if (!dialog.open) {
        if (typeof dialog.showModal === 'function') {
          dialog.showModal();
        } else {
          dialog.setAttribute('open', '');
        }
        // The browser honours [autofocus] when the dialog is shown, but not
        // every engine does - make the initial focus deterministic.
        const initial = dialog.querySelector<HTMLElement>('[autofocus]');
        if (initial) {
          requestAnimationFrame(() => initial.focus());
        }
      }
    } else if (dialog.open) {
      dialog.close();
    }
  }, [open]);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    const handleClose = () => onCloseRef.current();
    dialog.addEventListener('close', handleClose);
    return () => dialog.removeEventListener('close', handleClose);
  }, []);

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby={titleId}
      closedby={closedby}
      className={`m-auto max-h-[92dvh] w-full overflow-y-auto bg-transparent p-4 text-slate-100 backdrop:bg-black/75 backdrop:backdrop-blur-md ${className}`}
    >
      {children}
    </dialog>
  );
}
