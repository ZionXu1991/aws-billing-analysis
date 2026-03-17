-- Add market column to account_metadata
ALTER TABLE billing.account_metadata ADD COLUMN IF NOT EXISTS market VARCHAR(255) NOT NULL DEFAULT 'UNKNOWN';
CREATE INDEX IF NOT EXISTS idx_account_metadata_market ON billing.account_metadata(market);
CREATE INDEX IF NOT EXISTS idx_account_metadata_market_env ON billing.account_metadata(market, environment);

-- Add market column to monthly_cost_summary
ALTER TABLE billing.monthly_cost_summary ADD COLUMN IF NOT EXISTS market VARCHAR(255);

-- Add market column to budget
ALTER TABLE billing.budget ADD COLUMN IF NOT EXISTS market VARCHAR(255);
