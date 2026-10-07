# LORY backend: deployment notes

## Database update

Before starting the updated API, run `migrations/20261007_password_recovery_and_group_membership.sql` once against the existing `lory_db`. It adds the one-use password recovery table, the chair-scoped unique group membership constraint, and poll-question relationships; existing polls and votes are migrated as one question per poll.

The migration intentionally preserves existing group memberships. If the unique membership constraint reports duplicates, resolve those duplicate assignments within each chair before rerunning the migration.

## Password recovery email

Configure these variables in the PyCharm run configuration's environment before starting the API. `.env.example` is a template and is not loaded automatically.

| Variable | Value |
| --- | --- |
| `SMTP_HOST` | SMTP server provided by the email provider |
| `SMTP_PORT` | Usually `587` for STARTTLS |
| `SMTP_STARTTLS` | `true` when the provider requires STARTTLS |
| `SMTP_USER` | SMTP account username |
| `SMTP_PASSWORD` | Provider-generated SMTP/app password; do not use a personal account password |
| `SMTP_FROM` | Sender address accepted by that provider |
| `PASSWORD_RESET_SECRET` | Long random secret used to hash recovery PINs and reset tokens |

Generate `PASSWORD_RESET_SECRET` locally with Python's `secrets` module and keep it private. Recovery PINs expire after seven minutes and are limited to five attempts. The API responds generically whether or not an account exists.
