-- Migration to add allow_notes to subchecks and rename custom_instructions to description
ALTER TABLE checkpoints
    RENAME COLUMN custom_instructions TO description;
ALTER TABLE checkpoint_sub_checks
    ADD COLUMN allow_notes BOOLEAN DEFAULT TRUE;
