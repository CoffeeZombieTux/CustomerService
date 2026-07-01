-- Deleting a customer cascades (via JPA) to their bonus_account, but credit_transactions
-- reference bonus_accounts without ON DELETE CASCADE, so the delete was blocked by a FK
-- violation once the customer had any bonus history. Recreate the FK with ON DELETE CASCADE
-- so removing a bonus account also removes its transaction ledger.
ALTER TABLE credit_transactions
    DROP CONSTRAINT credit_transactions_bonus_account_id_fkey;

ALTER TABLE credit_transactions
    ADD CONSTRAINT credit_transactions_bonus_account_id_fkey
        FOREIGN KEY (bonus_account_id) REFERENCES bonus_accounts(id) ON DELETE CASCADE;
