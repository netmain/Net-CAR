# Security Specification (`security_spec.md`)

## 1. Data Invariants
1. **Strict Tenant Isolation**: Every document under `/users/{userId}` and its subcollections (`vehicles`, `fillups`, `expenses`, `services`, `reminders`) belongs strictly to `userId` (`request.auth.uid == userId`). No user can read, list, create, update, or delete another user's profile or subcollection documents.
2. **Relational Parent Existence (Master Gate)**: A subcollection document (`vehicles`, `fillups`, `expenses`, `services`, `reminders`) can only be accessed or mutated if its `userId` matches `request.auth.uid` and the parent `/users/{userId}` profile exists (or for child telemetry records, the referenced `/users/{userId}/vehicles/{vehicleId}` exists on create).
3. **Immutable Identity & Creation Metadata**: `id`, `userId`, and `createdAt` cannot be modified after creation. `createdAt` and `updatedAt` must match `request.time`.
4. **Strict Key Allowlisting & Volumetric Bounds**: Every `create` and `update` validates exact allowed keys (`hasOnly`) and required keys (`hasAll`), string length bounds (`.size() <= max`), and numeric domain bounds.

## 2. The "Dirty Dozen" Payloads
1. **Identity Spoofing on User Profile**: Authenticated user `uid_A` creates `/users/uid_B` or sets `userId: "uid_B"`. -> `PERMISSION_DENIED`
2. **Shadow Field Injection on Profile**: User updates `/users/uid_A` with `{ isAdmin: true }`. -> `PERMISSION_DENIED`
3. **Cross-Tenant PII Read**: Authenticated user `uid_B` attempts `get` or `list` on `/users/uid_A` or `/users/uid_A/vehicles`. -> `PERMISSION_DENIED`
4. **Orphaned FillUp Creation**: User `uid_A` creates `/users/uid_A/fillups/f1` referencing a non-existent `vehicleId: "non-existent"`. -> `PERMISSION_DENIED`
5. **Immutable Field Tampering**: User `uid_A` updates `createdAt` or `userId` on `/users/uid_A/vehicles/veh-1`. -> `PERMISSION_DENIED`
6. **Future/Past Forged Timestamp**: User `uid_A` sends a hardcoded client timestamp instead of `request.time` for `createdAt` or `updatedAt`. -> `PERMISSION_DENIED`
7. **Denial of Wallet String Overflow**: User `uid_A` creates a vehicle with a 10,000-character `make` or `notes` string. -> `PERMISSION_DENIED`
8. **Path ID Poisoning**: User `uid_A` creates a vehicle with an invalid document ID containing spaces or special characters. -> `PERMISSION_DENIED`
9. **Negative Odometer / Fuel Volume**: User `uid_A` creates a fill-up with `volume: -10` or `odometer: -5`. -> `PERMISSION_DENIED`
10. **Unbounded Array Injection**: User `uid_A` creates a service record with 100 items in `parts` or non-string elements. -> `PERMISSION_DENIED`
11. **Value Poisoning on Update**: User `uid_A` updates `currentOdometer` on a vehicle to a string `"65000"` instead of a number. -> `PERMISSION_DENIED`
12. **Unauthenticated Access**: Unauthenticated client attempts to read or write any document under `/users/{userId}`. -> `PERMISSION_DENIED`
