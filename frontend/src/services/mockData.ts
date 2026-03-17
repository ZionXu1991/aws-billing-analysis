import type {
  OverviewData,
  TrendPoint,
  ServiceCostItem,
  EnvironmentCostItem,
  AccountCostItem,
  AnomalyItem,
  AnomalyPage,
  AccountItem,
  BudgetItem,
  MarketCostResponse,
  MarketEnvironmentCostResponse,
  MarketTrendResponse,
  CostDriverResponse,
  CostMoverResponse,
  BurnRateResponse,
  MarketBudgetResponse,
  CostAnalysisRequest,
  CostAnalysisResponse,
  FilterOptionsResponse,
} from './billingApi';

// ── Helper ──

const delay = (ms = 300) => new Promise((r) => setTimeout(r, ms));

const teams = ['Platform', 'Data', 'Frontend', 'Backend', 'Mobile', 'DevOps', 'Security', 'ML'];
const regions = ['us-east-1', 'us-west-2', 'ap-southeast-1', 'eu-west-1', 'ap-northeast-1'];
const services = [
  'Amazon EC2', 'Amazon S3', 'Amazon RDS', 'AWS Lambda', 'Amazon CloudFront',
  'Amazon DynamoDB', 'Amazon EKS', 'Amazon ElastiCache', 'AWS Fargate', 'Amazon SQS',
  'Amazon Kinesis', 'Amazon Redshift', 'AWS Glue', 'Amazon SageMaker', 'Amazon API Gateway',
];
const envs: Array<'DEV' | 'PREPROD' | 'PROD'> = ['DEV', 'PREPROD', 'PROD'];
const markets = ['US', 'UK', 'DE', 'FR', 'JP', 'SG', 'AU', 'BR', 'IN', 'CA'];

function rand(min: number, max: number) {
  return Math.random() * (max - min) + min;
}

// ── Mock Accounts (30 accounts) ──

let nextAccountId = 1;
const mockAccounts: AccountItem[] = Array.from({ length: 30 }, (_, i) => ({
  id: ++nextAccountId,
  accountId: String(100000000001 + i),
  accountName: `${markets[Math.floor(i / 3)]}-${envs[i % 3].toLowerCase()}-account`,
  team: teams[i % teams.length],
  environment: envs[i % 3],
  region: regions[i % regions.length],
  market: markets[Math.floor(i / 3)],
  active: true,
}));

// ── Mock Anomalies ──

const anomalyTypes = ['SPIKE', 'NEW_SERVICE', 'ZERO_COST', 'BUDGET_EXCEED'];
const severities: AnomalyItem['severity'][] = ['CRITICAL', 'HIGH', 'MEDIUM'];
const statuses: AnomalyItem['status'][] = ['OPEN', 'ACKNOWLEDGED', 'RESOLVED'];

const mockAnomalies: AnomalyItem[] = Array.from({ length: 47 }, (_, i) => {
  const acct = mockAccounts[i % mockAccounts.length];
  const expected = rand(50, 500);
  const actual = expected * rand(1.5, 5);
  return {
    id: i + 1,
    accountId: acct.accountId,
    accountName: acct.accountName,
    serviceName: services[i % services.length],
    anomalyType: anomalyTypes[i % anomalyTypes.length],
    severity: severities[i % severities.length],
    status: statuses[i % statuses.length],
    detectedDate: `2026-03-${String(Math.max(1, 17 - Math.floor(i / 3))).padStart(2, '0')}`,
    expectedCost: Math.round(expected * 100) / 100,
    actualCost: Math.round(actual * 100) / 100,
    deviationPercent: Math.round(((actual - expected) / expected) * 100 * 10) / 10,
    description: `${anomalyTypes[i % anomalyTypes.length]} detected for ${services[i % services.length]} in ${acct.accountName}`,
  };
});

// ── Mock Budgets ──

let nextBudgetId = 1;
const mockBudgets: BudgetItem[] = [
  { id: nextBudgetId++, accountId: null, serviceName: null, market: null, yearMonth: '2026-03', budgetAmount: 150000, actualAmount: 98450, utilizationPercent: 65.6, alertThresholdPercent: 80 },
  { id: nextBudgetId++, accountId: null, serviceName: 'Amazon EC2', market: null, yearMonth: '2026-03', budgetAmount: 45000, actualAmount: 38200, utilizationPercent: 84.9, alertThresholdPercent: 80 },
  { id: nextBudgetId++, accountId: null, serviceName: 'Amazon RDS', market: null, yearMonth: '2026-03', budgetAmount: 25000, actualAmount: 18900, utilizationPercent: 75.6, alertThresholdPercent: 80 },
  { id: nextBudgetId++, accountId: '100000000001', serviceName: null, market: 'US', yearMonth: '2026-03', budgetAmount: 8000, actualAmount: 9200, utilizationPercent: 115.0, alertThresholdPercent: 80 },
  { id: nextBudgetId++, accountId: '100000000004', serviceName: null, market: 'UK', yearMonth: '2026-03', budgetAmount: 12000, actualAmount: 10500, utilizationPercent: 87.5, alertThresholdPercent: 80 },
  { id: nextBudgetId++, accountId: null, serviceName: 'Amazon S3', market: null, yearMonth: '2026-03', budgetAmount: 8000, actualAmount: 5320, utilizationPercent: 66.5, alertThresholdPercent: 80 },
  { id: nextBudgetId++, accountId: null, serviceName: null, market: null, yearMonth: '2026-02', budgetAmount: 140000, actualAmount: 142300, utilizationPercent: 101.6, alertThresholdPercent: 80 },
  { id: nextBudgetId++, accountId: null, serviceName: 'AWS Lambda', market: null, yearMonth: '2026-03', budgetAmount: 5000, actualAmount: 2800, utilizationPercent: 56.0, alertThresholdPercent: 80 },
];

// ── API Mock Implementations ──

export async function getOverview(_yearMonth: string): Promise<OverviewData> {
  await delay();
  return {
    mtdTotalCost: 98450.32,
    projectedMonthEndCost: 156280.50,
    activeAnomalies: mockAnomalies.filter((a) => a.status === 'OPEN').length,
    budgetUtilizationPercent: 65.6,
    momChangePercent: 8.3,
  };
}

export async function getTrend(): Promise<TrendPoint[]> {
  await delay();
  const points: TrendPoint[] = [];
  const months = ['2025-04','2025-05','2025-06','2025-07','2025-08','2025-09','2025-10','2025-11','2025-12','2026-01','2026-02','2026-03'];
  const baseCosts = { DEV: 15000, PREPROD: 25000, PROD: 80000 };
  for (const ym of months) {
    for (const env of envs) {
      const base = baseCosts[env];
      const monthIdx = months.indexOf(ym);
      const growth = 1 + monthIdx * 0.02 + rand(-0.05, 0.05);
      points.push({
        yearMonth: ym,
        environment: env,
        totalCost: Math.round(base * growth * 100) / 100,
      });
    }
  }
  return points;
}

export async function getTopServices(_yearMonth: string, _limit = 10): Promise<ServiceCostItem[]> {
  await delay();
  const costs = services.slice(0, 10).map((s) => ({
    serviceName: s,
    totalCost: Math.round(rand(2000, 35000) * 100) / 100,
    percentage: 0,
  }));
  costs.sort((a, b) => b.totalCost - a.totalCost);
  const total = costs.reduce((s, c) => s + c.totalCost, 0);
  return costs.map((c) => ({ ...c, percentage: Math.round((c.totalCost / total) * 1000) / 10 }));
}

export async function getEnvironmentDistribution(_yearMonth: string): Promise<EnvironmentCostItem[]> {
  await delay();
  return [
    { environment: 'PROD', totalCost: 78200, percentage: 63.5 },
    { environment: 'PREPROD', totalCost: 28400, percentage: 23.1 },
    { environment: 'DEV', totalCost: 16500, percentage: 13.4 },
  ];
}

export async function getAccountCosts(_yearMonth: string): Promise<AccountCostItem[]> {
  await delay();
  return mockAccounts.map((a) => ({
    accountId: a.accountId,
    accountName: a.accountName,
    team: a.team,
    environment: a.environment,
    market: a.market,
    mtdCost: Math.round(rand(800, 12000) * 100) / 100,
    momChangePercent: Math.round(rand(-25, 35) * 10) / 10,
  }));
}

export async function getRecentAnomalies(limit = 5): Promise<AnomalyItem[]> {
  await delay();
  return mockAnomalies.slice(0, limit);
}

export async function getAnomalies(params: {
  page?: number;
  size?: number;
  severity?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
}): Promise<AnomalyPage> {
  await delay();
  let filtered = [...mockAnomalies];
  if (params.severity) filtered = filtered.filter((a) => a.severity === params.severity);
  if (params.status) filtered = filtered.filter((a) => a.status === params.status);
  const page = params.page ?? 0;
  const size = params.size ?? 10;
  const start = page * size;
  return {
    content: filtered.slice(start, start + size),
    totalElements: filtered.length,
    totalPages: Math.ceil(filtered.length / size),
    number: page,
    size,
  };
}

export async function acknowledgeAnomaly(id: number): Promise<void> {
  await delay();
  const a = mockAnomalies.find((x) => x.id === id);
  if (a) a.status = 'ACKNOWLEDGED';
}

export async function getAccounts(): Promise<AccountItem[]> {
  await delay();
  return mockAccounts.filter((a) => a.active);
}

export async function createAccount(account: Omit<AccountItem, 'id' | 'active'>): Promise<AccountItem> {
  await delay();
  const newAccount: AccountItem = { ...account, id: ++nextAccountId, active: true };
  mockAccounts.push(newAccount);
  return newAccount;
}

export async function updateAccount(id: number, account: Partial<AccountItem>): Promise<AccountItem> {
  await delay();
  const idx = mockAccounts.findIndex((a) => a.id === id);
  if (idx >= 0) Object.assign(mockAccounts[idx], account);
  return mockAccounts[idx];
}

export async function deleteAccount(id: number): Promise<void> {
  await delay();
  const a = mockAccounts.find((x) => x.id === id);
  if (a) a.active = false;
}

export async function getBudgets(_yearMonth: string): Promise<BudgetItem[]> {
  await delay();
  return mockBudgets.filter((b) => b.yearMonth === _yearMonth);
}

export async function createBudget(budget: Omit<BudgetItem, 'id' | 'actualAmount' | 'utilizationPercent'>): Promise<BudgetItem> {
  await delay();
  const newBudget: BudgetItem = {
    ...budget,
    id: ++nextBudgetId,
    actualAmount: Math.round(budget.budgetAmount * rand(0.3, 0.9) * 100) / 100,
    utilizationPercent: Math.round(rand(30, 90) * 10) / 10,
  };
  mockBudgets.push(newBudget);
  return newBudget;
}

export async function updateBudget(id: number, budget: Partial<BudgetItem>): Promise<BudgetItem> {
  await delay();
  const idx = mockBudgets.findIndex((b) => b.id === id);
  if (idx >= 0) Object.assign(mockBudgets[idx], budget);
  return mockBudgets[idx];
}

export async function deleteBudget(id: number): Promise<void> {
  await delay();
  const idx = mockBudgets.findIndex((b) => b.id === id);
  if (idx >= 0) mockBudgets.splice(idx, 1);
}

export async function getDailyCosts(_yearMonth: string) {
  await delay();
  return [];
}

// ── Market Analysis Mock Implementations ──

export async function getMarketCosts(_yearMonth: string): Promise<MarketCostResponse> {
  await delay();
  const items = markets.map((market) => {
    const cost = Math.round(rand(5000, 25000) * 100) / 100;
    return {
      market,
      cost,
      percentage: 0,
      momChangePercent: Math.round(rand(-15, 25) * 10) / 10,
      accountCount: 3,
    };
  });
  const totalCost = items.reduce((s, c) => s + c.cost, 0);
  items.forEach((item) => {
    item.percentage = Math.round((item.cost / totalCost) * 1000) / 10;
  });
  return { items, totalCost: Math.round(totalCost * 100) / 100 };
}

export async function getMarketEnvironmentMatrix(_yearMonth: string): Promise<MarketEnvironmentCostResponse> {
  await delay();
  const items: MarketEnvironmentCostResponse['items'] = [];
  for (const market of markets) {
    for (const env of envs) {
      const multiplier = env === 'PROD' ? 3 : env === 'PREPROD' ? 1.5 : 1;
      items.push({
        market,
        environment: env,
        cost: Math.round(rand(1000, 8000) * multiplier * 100) / 100,
      });
    }
  }
  return { items, markets: [...markets], environments: [...envs] };
}

export async function getMarketTrend(_months = 12): Promise<MarketTrendResponse> {
  await delay();
  const months = ['2025-04','2025-05','2025-06','2025-07','2025-08','2025-09','2025-10','2025-11','2025-12','2026-01','2026-02','2026-03'];
  const data: MarketTrendResponse['data'] = [];
  for (const ym of months) {
    const monthIdx = months.indexOf(ym);
    for (const market of markets) {
      const base = rand(5000, 20000);
      const growth = 1 + monthIdx * 0.015 + rand(-0.03, 0.03);
      data.push({
        yearMonth: ym,
        market,
        cost: Math.round(base * growth * 100) / 100,
      });
    }
  }
  return { data, markets: [...markets] };
}

export async function getTopCostDrivers(_yearMonth: string, limit = 10): Promise<CostDriverResponse> {
  await delay();
  const items: CostDriverResponse['items'] = [];
  for (let i = 0; i < limit; i++) {
    items.push({
      market: markets[i % markets.length],
      serviceName: services[i % services.length],
      cost: Math.round(rand(3000, 30000) * 100) / 100,
      percentage: Math.round(rand(2, 15) * 10) / 10,
      momChangePercent: Math.round(rand(-10, 30) * 10) / 10,
    });
  }
  items.sort((a, b) => b.cost - a.cost);
  return { items };
}

export async function getTopCostMovers(_yearMonth: string, limit = 5): Promise<CostMoverResponse> {
  await delay();
  const items: CostMoverResponse['items'] = [];
  for (let i = 0; i < limit; i++) {
    const prev = rand(2000, 15000);
    const current = prev * rand(1.2, 2.5);
    items.push({
      market: markets[i % markets.length],
      serviceName: services[i % services.length],
      currentCost: Math.round(current * 100) / 100,
      previousCost: Math.round(prev * 100) / 100,
      changeAmount: Math.round((current - prev) * 100) / 100,
      changePercent: Math.round(((current - prev) / prev) * 100 * 10) / 10,
    });
  }
  items.sort((a, b) => b.changeAmount - a.changeAmount);
  return { items };
}

export async function getBurnRate(_market: string, _yearMonth: string): Promise<BurnRateResponse> {
  await delay();
  const items: BurnRateResponse['items'] = [];
  let cumulative = 0;
  const daysInMonth = 31;
  for (let d = 1; d <= 17; d++) {
    const daily = rand(300, 900);
    cumulative += daily;
    const avgDaily = cumulative / d;
    items.push({
      date: `2026-03-${String(d).padStart(2, '0')}`,
      dailyCost: Math.round(daily * 100) / 100,
      cumulativeCost: Math.round(cumulative * 100) / 100,
      projectedMonthEnd: Math.round(avgDaily * daysInMonth * 100) / 100,
    });
  }
  const avgDailyBurn = cumulative / 17;
  return {
    market: _market,
    yearMonth: _yearMonth,
    items,
    averageDailyBurn: Math.round(avgDailyBurn * 100) / 100,
    projectedMonthEnd: Math.round(avgDailyBurn * daysInMonth * 100) / 100,
  };
}

export async function getBudgetVsActualByMarket(_yearMonth: string): Promise<MarketBudgetResponse> {
  await delay();
  const items = markets.map((market) => {
    const budget = rand(10000, 30000);
    const actual = budget * rand(0.5, 1.2);
    return {
      market,
      budgetAmount: Math.round(budget * 100) / 100,
      actualAmount: Math.round(actual * 100) / 100,
      utilizationPercent: Math.round((actual / budget) * 100 * 10) / 10,
      remainingAmount: Math.round((budget - actual) * 100) / 100,
    };
  });
  return { items };
}

export async function getMarketList(): Promise<string[]> {
  await delay();
  return [...markets];
}

export async function analyzeCosts(request: CostAnalysisRequest): Promise<CostAnalysisResponse> {
  await delay();
  const groupBy = request.groupBy || 'market';
  let groups: string[];
  switch (groupBy) {
    case 'service': groups = services.slice(0, 10); break;
    case 'account': groups = mockAccounts.slice(0, 10).map((a) => a.accountId); break;
    case 'environment': groups = [...envs]; break;
    default: groups = [...markets]; break;
  }
  const items = groups.map((g) => {
    const cost = Math.round(rand(2000, 30000) * 100) / 100;
    return {
      group: g,
      cost,
      percentage: 0,
      momChangePercent: Math.round(rand(-20, 30) * 10) / 10,
    };
  });
  const totalCost = items.reduce((s, c) => s + c.cost, 0);
  items.forEach((item) => {
    item.percentage = Math.round((item.cost / totalCost) * 1000) / 10;
  });
  items.sort((a, b) => b.cost - a.cost);
  return { items, totalCost: Math.round(totalCost * 100) / 100, groupBy };
}

export async function getFilterOptions(): Promise<FilterOptionsResponse> {
  await delay();
  return {
    markets: [...markets],
    environments: [...envs],
    services: services.slice(0, 10),
    accounts: mockAccounts.map((a) => a.accountId),
  };
}
