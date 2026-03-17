import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1/billing',
});

// ── Type Definitions ──

export interface OverviewData {
  mtdTotalCost: number;
  projectedMonthEndCost: number;
  activeAnomalies: number;
  budgetUtilizationPercent: number;
  momChangePercent: number;
}

export interface TrendPoint {
  yearMonth: string;
  environment: string;
  totalCost: number;
}

export interface ServiceCostItem {
  serviceName: string;
  totalCost: number;
  percentage: number;
}

export interface AccountCostItem {
  accountId: string;
  accountName: string;
  team: string;
  environment: string;
  market: string;
  mtdCost: number;
  momChangePercent: number;
}

export interface EnvironmentCostItem {
  environment: string;
  totalCost: number;
  percentage: number;
}

export interface DailyCostItem {
  date: string;
  totalCost: number;
  serviceName: string;
}

export interface AnomalyItem {
  id: number;
  accountId: string;
  accountName: string;
  serviceName: string;
  anomalyType: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  status: 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED';
  detectedDate: string;
  expectedCost: number;
  actualCost: number;
  deviationPercent: number;
  description: string;
}

export interface AnomalyPage {
  content: AnomalyItem[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface AccountItem {
  id: number;
  accountId: string;
  accountName: string;
  team: string;
  environment: string;
  region: string;
  market: string;
  active: boolean;
}

export interface BudgetItem {
  id: number;
  accountId: string | null;
  serviceName: string | null;
  market: string | null;
  yearMonth: string;
  budgetAmount: number;
  actualAmount: number;
  utilizationPercent: number;
  alertThresholdPercent: number;
}

// ── API Functions ──

export async function getOverview(yearMonth: string): Promise<OverviewData> {
  const { data } = await api.get<OverviewData>('/overview', { params: { yearMonth } });
  return data;
}

export async function getTrend(): Promise<TrendPoint[]> {
  const { data } = await api.get<TrendPoint[]>('/trend');
  return data;
}

export async function getTopServices(yearMonth: string, limit = 10): Promise<ServiceCostItem[]> {
  const { data } = await api.get<ServiceCostItem[]>('/services/top', {
    params: { yearMonth, limit },
  });
  return data;
}

export async function getEnvironmentDistribution(yearMonth: string): Promise<EnvironmentCostItem[]> {
  const { data } = await api.get<EnvironmentCostItem[]>('/environments', {
    params: { yearMonth },
  });
  return data;
}

export async function getAccountCosts(yearMonth: string): Promise<AccountCostItem[]> {
  const { data } = await api.get<AccountCostItem[]>('/accounts/costs', {
    params: { yearMonth },
  });
  return data;
}

export async function getDailyCosts(yearMonth: string): Promise<DailyCostItem[]> {
  const { data } = await api.get<DailyCostItem[]>('/daily', { params: { yearMonth } });
  return data;
}

// Anomalies
export async function getAnomalies(params: {
  page?: number;
  size?: number;
  severity?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
}): Promise<AnomalyPage> {
  const { data } = await api.get<AnomalyPage>('/anomalies', { params });
  return data;
}

export async function getRecentAnomalies(limit = 5): Promise<AnomalyItem[]> {
  const { data } = await api.get<AnomalyItem[]>('/anomalies/recent', { params: { limit } });
  return data;
}

export async function acknowledgeAnomaly(id: number): Promise<void> {
  await api.put(`/anomalies/${id}/acknowledge`);
}

// Accounts
export async function getAccounts(): Promise<AccountItem[]> {
  const { data } = await api.get<AccountItem[]>('/accounts');
  return data;
}

export async function createAccount(account: Omit<AccountItem, 'id' | 'active'>): Promise<AccountItem> {
  const { data } = await api.post<AccountItem>('/accounts', account);
  return data;
}

export async function updateAccount(id: number, account: Partial<AccountItem>): Promise<AccountItem> {
  const { data } = await api.put<AccountItem>(`/accounts/${id}`, account);
  return data;
}

export async function deleteAccount(id: number): Promise<void> {
  await api.delete(`/accounts/${id}`);
}

// Budgets
export async function getBudgets(yearMonth: string): Promise<BudgetItem[]> {
  const { data } = await api.get<BudgetItem[]>('/budgets', { params: { yearMonth } });
  return data;
}

export async function createBudget(budget: Omit<BudgetItem, 'id' | 'actualAmount' | 'utilizationPercent'>): Promise<BudgetItem> {
  const { data } = await api.post<BudgetItem>('/budgets', budget);
  return data;
}

export async function updateBudget(id: number, budget: Partial<BudgetItem>): Promise<BudgetItem> {
  const { data } = await api.put<BudgetItem>(`/budgets/${id}`, budget);
  return data;
}

export async function deleteBudget(id: number): Promise<void> {
  await api.delete(`/budgets/${id}`);
}

// ── Market Analysis Types ──

export interface MarketCostItem {
  market: string;
  cost: number;
  percentage: number;
  momChangePercent: number;
  accountCount: number;
}

export interface MarketCostResponse {
  items: MarketCostItem[];
  totalCost: number;
}

export interface MarketEnvironmentCostItem {
  market: string;
  environment: string;
  cost: number;
}

export interface MarketEnvironmentCostResponse {
  items: MarketEnvironmentCostItem[];
  markets: string[];
  environments: string[];
}

export interface MarketTrendPoint {
  yearMonth: string;
  market: string;
  cost: number;
}

export interface MarketTrendResponse {
  data: MarketTrendPoint[];
  markets: string[];
}

export interface CostDriverItem {
  market: string;
  serviceName: string;
  cost: number;
  percentage: number;
  momChangePercent: number;
}

export interface CostDriverResponse {
  items: CostDriverItem[];
}

export interface CostMoverItem {
  market: string;
  serviceName: string;
  currentCost: number;
  previousCost: number;
  changeAmount: number;
  changePercent: number;
}

export interface CostMoverResponse {
  items: CostMoverItem[];
}

export interface BurnRateItem {
  date: string;
  dailyCost: number;
  cumulativeCost: number;
  projectedMonthEnd: number;
}

export interface BurnRateResponse {
  market: string;
  yearMonth: string;
  items: BurnRateItem[];
  averageDailyBurn: number;
  projectedMonthEnd: number;
}

export interface MarketBudgetItem {
  market: string;
  budgetAmount: number;
  actualAmount: number;
  utilizationPercent: number;
  remainingAmount: number;
}

export interface MarketBudgetResponse {
  items: MarketBudgetItem[];
}

export interface CostAnalysisRequest {
  markets?: string[];
  environments?: string[];
  services?: string[];
  accounts?: string[];
  startYearMonth?: string;
  endYearMonth?: string;
  groupBy?: string;
}

export interface CostAnalysisItem {
  group: string;
  cost: number;
  percentage: number;
  momChangePercent: number;
}

export interface CostAnalysisResponse {
  items: CostAnalysisItem[];
  totalCost: number;
  groupBy: string;
}

export interface FilterOptionsResponse {
  markets: string[];
  environments: string[];
  services: string[];
  accounts: string[];
}

// ── Market Analysis API Functions ──

export async function getMarketCosts(yearMonth: string): Promise<MarketCostResponse> {
  const { data } = await api.get<MarketCostResponse>('/markets/costs', { params: { yearMonth } });
  return data;
}

export async function getMarketEnvironmentMatrix(yearMonth: string): Promise<MarketEnvironmentCostResponse> {
  const { data } = await api.get<MarketEnvironmentCostResponse>('/markets/matrix', { params: { yearMonth } });
  return data;
}

export async function getMarketTrend(months = 12): Promise<MarketTrendResponse> {
  const { data } = await api.get<MarketTrendResponse>('/markets/trend', { params: { months } });
  return data;
}

export async function getTopCostDrivers(yearMonth: string, limit = 10): Promise<CostDriverResponse> {
  const { data } = await api.get<CostDriverResponse>('/markets/drivers', { params: { yearMonth, limit } });
  return data;
}

export async function getTopCostMovers(yearMonth: string, limit = 5): Promise<CostMoverResponse> {
  const { data } = await api.get<CostMoverResponse>('/markets/movers', { params: { yearMonth, limit } });
  return data;
}

export async function getBurnRate(market: string, yearMonth: string): Promise<BurnRateResponse> {
  const { data } = await api.get<BurnRateResponse>(`/markets/${market}/burn-rate`, { params: { yearMonth } });
  return data;
}

export async function getBudgetVsActualByMarket(yearMonth: string): Promise<MarketBudgetResponse> {
  const { data } = await api.get<MarketBudgetResponse>('/markets/budget-vs-actual', { params: { yearMonth } });
  return data;
}

export async function getMarketList(): Promise<string[]> {
  const { data } = await api.get<string[]>('/markets/list');
  return data;
}

// ── Cost Analysis API Functions ──

export async function analyzeCosts(request: CostAnalysisRequest): Promise<CostAnalysisResponse> {
  const { data } = await api.post<CostAnalysisResponse>('/analysis/query', request);
  return data;
}

export async function getFilterOptions(): Promise<FilterOptionsResponse> {
  const { data } = await api.get<FilterOptionsResponse>('/analysis/filters');
  return data;
}
