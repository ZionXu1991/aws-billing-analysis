import React, { useCallback, useEffect, useState } from 'react';
import {
  Row,
  Col,
  Card,
  Statistic,
  Select,
  Spin,
  Table,
  Tag,
  List,
  Typography,
  message,
} from 'antd';
import {
  ArrowUpOutlined,
  ArrowDownOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import { Pie, Bar, Area } from '@ant-design/charts';
import dayjs from 'dayjs';
import type { ColumnsType } from 'antd/es/table';
import {
  getOverview,
  getTrend,
  getTopServices,
  getEnvironmentDistribution,
  getAccountCosts,
  getRecentAnomalies,
} from '../services/mockData';
import type {
  OverviewData,
  TrendPoint,
  ServiceCostItem,
  EnvironmentCostItem,
  AccountCostItem,
  AnomalyItem,
} from '../services/billingApi';

const formatCost = (value: number): string =>
  `$${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

const generateMonthOptions = () => {
  const options: { label: string; value: string }[] = [];
  const now = dayjs();
  for (let i = 0; i < 12; i++) {
    const d = now.subtract(i, 'month');
    options.push({ label: d.format('YYYY-MM'), value: d.format('YYYY-MM') });
  }
  return options;
};

const envColors: Record<string, string> = {
  DEV: '#1890ff',
  PREPROD: '#fa8c16',
  PROD: '#f5222d',
  dev: '#1890ff',
  preprod: '#fa8c16',
  prod: '#f5222d',
};

const severityColors: Record<string, string> = {
  CRITICAL: 'red',
  HIGH: 'orange',
  MEDIUM: 'gold',
  LOW: 'blue',
};

const BillingDashboardPage: React.FC = () => {
  const [yearMonth, setYearMonth] = useState(dayjs().format('YYYY-MM'));
  const [loading, setLoading] = useState(true);
  const [overview, setOverview] = useState<OverviewData | null>(null);
  const [trend, setTrend] = useState<TrendPoint[]>([]);
  const [topServices, setTopServices] = useState<ServiceCostItem[]>([]);
  const [envDist, setEnvDist] = useState<EnvironmentCostItem[]>([]);
  const [accountCosts, setAccountCosts] = useState<AccountCostItem[]>([]);
  const [anomalies, setAnomalies] = useState<AnomalyItem[]>([]);
  const [accountSearch, setAccountSearch] = useState('');

  const fetchData = useCallback(async (ym: string) => {
    setLoading(true);
    try {
      const [ov, tr, svc, env, acc, ano] = await Promise.all([
        getOverview(ym),
        getTrend(),
        getTopServices(ym),
        getEnvironmentDistribution(ym),
        getAccountCosts(ym),
        getRecentAnomalies(5),
      ]);
      setOverview(ov);
      setTrend(tr);
      setTopServices(svc);
      setEnvDist(env);
      setAccountCosts(acc);
      setAnomalies(ano);
    } catch {
      message.error('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData(yearMonth);
  }, [yearMonth, fetchData]);

  const accountColumns: ColumnsType<AccountCostItem> = [
    { title: 'Account ID', dataIndex: 'accountId', key: 'accountId', sorter: (a, b) => a.accountId.localeCompare(b.accountId) },
    { title: 'Name', dataIndex: 'accountName', key: 'accountName', sorter: (a, b) => a.accountName.localeCompare(b.accountName) },
    { title: 'Market', dataIndex: 'market', key: 'market', sorter: (a, b) => a.market.localeCompare(b.market) },
    { title: 'Team', dataIndex: 'team', key: 'team', sorter: (a, b) => a.team.localeCompare(b.team) },
    {
      title: 'Environment',
      dataIndex: 'environment',
      key: 'environment',
      render: (env: string) => (
        <Tag color={envColors[env] || 'default'}>{env}</Tag>
      ),
      sorter: (a, b) => a.environment.localeCompare(b.environment),
    },
    {
      title: 'MTD Cost',
      dataIndex: 'mtdCost',
      key: 'mtdCost',
      render: (v: number) => formatCost(v),
      sorter: (a, b) => a.mtdCost - b.mtdCost,
      defaultSortOrder: 'descend',
    },
    {
      title: 'MoM Change%',
      dataIndex: 'momChangePercent',
      key: 'momChangePercent',
      render: (v: number) => (
        <span style={{ color: v >= 0 ? '#f5222d' : '#52c41a' }}>
          {v >= 0 ? <ArrowUpOutlined /> : <ArrowDownOutlined />} {Math.abs(v).toFixed(1)}%
        </span>
      ),
      sorter: (a, b) => a.momChangePercent - b.momChangePercent,
    },
  ];

  const filteredAccounts = accountCosts.filter(
    (a) =>
      a.accountId.toLowerCase().includes(accountSearch.toLowerCase()) ||
      a.accountName.toLowerCase().includes(accountSearch.toLowerCase()) ||
      a.team.toLowerCase().includes(accountSearch.toLowerCase()) ||
      a.market.toLowerCase().includes(accountSearch.toLowerCase()),
  );

  const pieConfig = {
    data: envDist.map((e) => ({ type: e.environment, value: e.totalCost })),
    angleField: 'value',
    colorField: 'type',
    color: envDist.map((e) => envColors[e.environment] || '#999'),
    radius: 0.9,
    innerRadius: 0.6,
    label: {
      type: 'inner' as const,
      content: '{name}: {percentage}',
    },
    interactions: [{ type: 'element-active' as const }],
  };

  const barConfig = {
    data: [...topServices].reverse(),
    xField: 'totalCost',
    yField: 'serviceName',
    seriesField: 'serviceName',
    color: '#1890ff',
    label: {
      position: 'right' as const,
      formatter: (datum: { totalCost?: number }) => formatCost(datum.totalCost ?? 0),
    },
  };

  const areaConfig = {
    data: trend,
    xField: 'yearMonth',
    yField: 'totalCost',
    seriesField: 'environment',
    isStack: true,
    color: (datum: { environment?: string }) => envColors[datum.environment ?? ''] || '#999',
    smooth: true,
    areaStyle: { fillOpacity: 0.6 },
    yAxis: {
      label: {
        formatter: (v: string) => `$${Number(v).toLocaleString()}`,
      },
    },
  };

  return (
    <Spin spinning={loading}>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          Billing Dashboard
        </Typography.Title>
        <Select
          value={yearMonth}
          onChange={setYearMonth}
          options={generateMonthOptions()}
          style={{ width: 160 }}
        />
      </div>

      {/* Row 1: Statistic Cards */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="MTD Total Cost"
              value={overview?.mtdTotalCost ?? 0}
              precision={2}
              prefix="$"
              suffix={
                overview ? (
                  <span style={{ fontSize: 14, color: overview.momChangePercent >= 0 ? '#f5222d' : '#52c41a' }}>
                    {overview.momChangePercent >= 0 ? <ArrowUpOutlined /> : <ArrowDownOutlined />}
                    {Math.abs(overview.momChangePercent).toFixed(1)}%
                  </span>
                ) : null
              }
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Projected Month-End Cost"
              value={overview?.projectedMonthEndCost ?? 0}
              precision={2}
              prefix="$"
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Active Anomalies"
              value={overview?.activeAnomalies ?? 0}
              valueStyle={{
                color: (overview?.activeAnomalies ?? 0) > 0 ? '#faad14' : undefined,
              }}
              prefix={(overview?.activeAnomalies ?? 0) > 0 ? <WarningOutlined /> : undefined}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Budget Utilization"
              value={overview?.budgetUtilizationPercent ?? 0}
              precision={1}
              suffix="%"
              valueStyle={{
                color:
                  (overview?.budgetUtilizationPercent ?? 0) > 100
                    ? '#f5222d'
                    : (overview?.budgetUtilizationPercent ?? 0) > 80
                      ? '#faad14'
                      : '#52c41a',
              }}
            />
          </Card>
        </Col>
      </Row>

      {/* Row 2: Env Distribution + Top Services */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={12}>
          <Card title="Environment Distribution">
            {envDist.length > 0 && <Pie {...pieConfig} height={300} />}
          </Card>
        </Col>
        <Col span={12}>
          <Card title="Top 10 Services">
            {topServices.length > 0 && <Bar {...barConfig} height={300} />}
          </Card>
        </Col>
      </Row>

      {/* Row 3: 12-Month Trend */}
      <Row style={{ marginBottom: 16 }}>
        <Col span={24}>
          <Card title="12-Month Cost Trend (by Environment)">
            {trend.length > 0 && <Area {...areaConfig} height={350} />}
          </Card>
        </Col>
      </Row>

      {/* Row 4: Account Costs Table */}
      <Row style={{ marginBottom: 16 }}>
        <Col span={24}>
          <Card
            title="Account Costs"
            extra={
              <input
                placeholder="Search accounts..."
                value={accountSearch}
                onChange={(e) => setAccountSearch(e.target.value)}
                style={{
                  padding: '4px 11px',
                  border: '1px solid #d9d9d9',
                  borderRadius: 6,
                  outline: 'none',
                  width: 240,
                }}
              />
            }
          >
            <Table
              dataSource={filteredAccounts}
              columns={accountColumns}
              rowKey="accountId"
              size="small"
              pagination={{ pageSize: 10 }}
            />
          </Card>
        </Col>
      </Row>

      {/* Row 5: Recent Anomalies */}
      <Row>
        <Col span={24}>
          <Card title="Recent Anomalies">
            <List
              dataSource={anomalies}
              renderItem={(item) => (
                <List.Item>
                  <List.Item.Meta
                    title={
                      <span>
                        <Tag color={severityColors[item.severity]}>{item.severity}</Tag>
                        {item.accountName} - {item.serviceName}
                      </span>
                    }
                    description={
                      <span>
                        {item.description} | Detected: {item.detectedDate} |
                        Expected: {formatCost(item.expectedCost)} |
                        Actual: {formatCost(item.actualCost)} |
                        Deviation: {item.deviationPercent.toFixed(1)}%
                      </span>
                    }
                  />
                  <Tag color={item.status === 'OPEN' ? 'red' : item.status === 'ACKNOWLEDGED' ? 'orange' : 'green'}>
                    {item.status}
                  </Tag>
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    </Spin>
  );
};

export default BillingDashboardPage;
