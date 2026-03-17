import React, { useCallback, useEffect, useState } from 'react';
import { Row, Col, Card, Statistic, Select, Spin, Table, Typography, Progress, message } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';
import { Bar, Line } from '@ant-design/charts';
import dayjs from 'dayjs';
import type { ColumnsType } from 'antd/es/table';
import {
  getMarketCosts,
  getMarketEnvironmentMatrix,
  getMarketTrend,
  getTopCostDrivers,
  getTopCostMovers,
  getBudgetVsActualByMarket,
} from '../services/mockData';
import type {
  CostDriverItem,
  CostMoverItem,
  MarketBudgetItem,
  MarketCostResponse,
  MarketEnvironmentCostResponse,
  MarketTrendResponse,
  CostDriverResponse,
  CostMoverResponse,
  MarketBudgetResponse,
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

const heatmapColor = (cost: number): string => {
  if (cost >= 20000) return '#ff4d4f';
  if (cost >= 15000) return '#ff7a45';
  if (cost >= 10000) return '#ffa940';
  if (cost >= 5000) return '#ffd666';
  return '#95de64';
};

const MarketOverviewPage: React.FC = () => {
  const [yearMonth, setYearMonth] = useState(dayjs().format('YYYY-MM'));
  const [loading, setLoading] = useState(true);
  const [marketCosts, setMarketCosts] = useState<MarketCostResponse | null>(null);
  const [matrix, setMatrix] = useState<MarketEnvironmentCostResponse | null>(null);
  const [trendData, setTrendData] = useState<MarketTrendResponse | null>(null);
  const [drivers, setDrivers] = useState<CostDriverResponse | null>(null);
  const [movers, setMovers] = useState<CostMoverResponse | null>(null);
  const [budgets, setBudgets] = useState<MarketBudgetResponse | null>(null);

  const fetchData = useCallback(async (ym: string) => {
    setLoading(true);
    try {
      const [mc, mx, tr, dr, mv, bg] = await Promise.all([
        getMarketCosts(ym),
        getMarketEnvironmentMatrix(ym),
        getMarketTrend(12),
        getTopCostDrivers(ym, 10),
        getTopCostMovers(ym, 5),
        getBudgetVsActualByMarket(ym),
      ]);
      setMarketCosts(mc);
      setMatrix(mx);
      setTrendData(tr);
      setDrivers(dr);
      setMovers(mv);
      setBudgets(bg);
    } catch {
      message.error('Failed to load market data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData(yearMonth);
  }, [yearMonth, fetchData]);

  // Top 5 markets by cost
  const topMarkets = marketCosts
    ? [...marketCosts.items].sort((a, b) => b.cost - a.cost).slice(0, 5)
    : [];

  // Heatmap table: rows = markets, columns = environments
  const heatmapDataMap = new Map<string, Record<string, number>>();
  if (matrix) {
    for (const item of matrix.items) {
      if (!heatmapDataMap.has(item.market)) {
        heatmapDataMap.set(item.market, {});
      }
      heatmapDataMap.get(item.market)![item.environment] = item.cost;
    }
  }
  const heatmapRows = matrix
    ? matrix.markets.map((market) => ({
        market,
        ...heatmapDataMap.get(market),
      }))
    : [];

  const heatmapColumns: ColumnsType<Record<string, unknown>> = [
    { title: 'Market', dataIndex: 'market', key: 'market', width: 80, fixed: 'left' as const },
    ...(matrix?.environments || []).map((env) => ({
      title: env,
      dataIndex: env,
      key: env,
      width: 120,
      render: (v: number) => (
        <div
          style={{
            background: heatmapColor(v || 0),
            padding: '4px 8px',
            borderRadius: 4,
            color: (v || 0) >= 15000 ? '#fff' : '#000',
            textAlign: 'center' as const,
            fontWeight: 500,
          }}
        >
          {formatCost(v || 0)}
        </div>
      ),
    })),
  ];

  // Bar chart data: market comparison
  const barData = marketCosts
    ? [...marketCosts.items].sort((a, b) => a.cost - b.cost).map((item) => ({
        market: item.market,
        cost: item.cost,
      }))
    : [];

  const barConfig = {
    data: barData,
    xField: 'cost',
    yField: 'market',
    color: '#1890ff',
    label: {
      position: 'right' as const,
      formatter: (datum: { cost?: number }) => formatCost(datum.cost ?? 0),
    },
  };

  // Line chart: trend per market
  const lineConfig = {
    data: trendData?.data || [],
    xField: 'yearMonth',
    yField: 'cost',
    seriesField: 'market',
    smooth: true,
    yAxis: {
      label: {
        formatter: (v: string) => `$${Number(v).toLocaleString()}`,
      },
    },
    legend: { position: 'top' as const },
  };

  // Cost drivers columns
  const driverColumns: ColumnsType<CostDriverItem> = [
    { title: 'Market', dataIndex: 'market', key: 'market', width: 80 },
    { title: 'Service', dataIndex: 'serviceName', key: 'serviceName' },
    {
      title: 'Cost',
      dataIndex: 'cost',
      key: 'cost',
      render: (v: number) => formatCost(v),
      sorter: (a, b) => a.cost - b.cost,
      defaultSortOrder: 'descend',
    },
    {
      title: '%',
      dataIndex: 'percentage',
      key: 'percentage',
      render: (v: number) => `${v.toFixed(1)}%`,
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

  // Cost movers columns
  const moverColumns: ColumnsType<CostMoverItem> = [
    { title: 'Market', dataIndex: 'market', key: 'market', width: 80 },
    { title: 'Service', dataIndex: 'serviceName', key: 'serviceName' },
    {
      title: 'Current',
      dataIndex: 'currentCost',
      key: 'currentCost',
      render: (v: number) => formatCost(v),
    },
    {
      title: 'Previous',
      dataIndex: 'previousCost',
      key: 'previousCost',
      render: (v: number) => formatCost(v),
    },
    {
      title: 'Change $',
      dataIndex: 'changeAmount',
      key: 'changeAmount',
      render: (v: number) => (
        <span style={{ color: v >= 0 ? '#f5222d' : '#52c41a' }}>
          {v >= 0 ? '+' : ''}{formatCost(v)}
        </span>
      ),
      sorter: (a, b) => a.changeAmount - b.changeAmount,
      defaultSortOrder: 'descend',
    },
    {
      title: 'Change %',
      dataIndex: 'changePercent',
      key: 'changePercent',
      render: (v: number) => (
        <span style={{ color: v >= 0 ? '#f5222d' : '#52c41a' }}>
          {v >= 0 ? <ArrowUpOutlined /> : <ArrowDownOutlined />} {Math.abs(v).toFixed(1)}%
        </span>
      ),
      sorter: (a, b) => a.changePercent - b.changePercent,
    },
  ];

  // Budget columns
  const budgetColumns: ColumnsType<MarketBudgetItem> = [
    { title: 'Market', dataIndex: 'market', key: 'market', width: 80 },
    {
      title: 'Budget',
      dataIndex: 'budgetAmount',
      key: 'budgetAmount',
      render: (v: number) => formatCost(v),
    },
    {
      title: 'Actual',
      dataIndex: 'actualAmount',
      key: 'actualAmount',
      render: (v: number) => formatCost(v),
    },
    {
      title: 'Utilization',
      dataIndex: 'utilizationPercent',
      key: 'utilizationPercent',
      width: 250,
      render: (v: number) => (
        <Progress
          percent={Math.min(v, 100)}
          format={() => `${v.toFixed(1)}%`}
          status={v > 100 ? 'exception' : v > 80 ? 'active' : 'normal'}
          strokeColor={v > 100 ? '#f5222d' : v > 80 ? '#faad14' : '#52c41a'}
        />
      ),
      sorter: (a, b) => a.utilizationPercent - b.utilizationPercent,
    },
    {
      title: 'Remaining',
      dataIndex: 'remainingAmount',
      key: 'remainingAmount',
      render: (v: number) => (
        <span style={{ color: v < 0 ? '#f5222d' : '#52c41a' }}>
          {formatCost(v)}
        </span>
      ),
    },
  ];

  return (
    <Spin spinning={loading}>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          Market Overview
        </Typography.Title>
        <Select
          value={yearMonth}
          onChange={setYearMonth}
          options={generateMonthOptions()}
          style={{ width: 160 }}
        />
      </div>

      {/* Row 1: Top 5 Market Stat Cards */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        {topMarkets.map((item) => (
          <Col span={Math.floor(24 / 5)} key={item.market}>
            <Card>
              <Statistic
                title={item.market}
                value={item.cost}
                precision={2}
                prefix="$"
                suffix={
                  <span style={{ fontSize: 14, color: item.momChangePercent >= 0 ? '#f5222d' : '#52c41a' }}>
                    {item.momChangePercent >= 0 ? <ArrowUpOutlined /> : <ArrowDownOutlined />}
                    {Math.abs(item.momChangePercent).toFixed(1)}%
                  </span>
                }
              />
              <div style={{ marginTop: 8, color: '#999', fontSize: 12 }}>
                {item.accountCount} accounts | {item.percentage.toFixed(1)}% of total
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      {/* Row 2: Heatmap + Bar Chart */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={14}>
          <Card title="Market x Environment Cost Heatmap">
            <Table
              dataSource={heatmapRows}
              columns={heatmapColumns}
              rowKey="market"
              size="small"
              pagination={false}
              scroll={{ x: 'max-content' }}
            />
          </Card>
        </Col>
        <Col span={10}>
          <Card title="Market Cost Comparison">
            {barData.length > 0 && <Bar {...barConfig} height={380} />}
          </Card>
        </Col>
      </Row>

      {/* Row 3: Trend Chart */}
      <Row style={{ marginBottom: 16 }}>
        <Col span={24}>
          <Card title="12-Month Market Cost Trend">
            {trendData && trendData.data.length > 0 && <Line {...lineConfig} height={400} />}
          </Card>
        </Col>
      </Row>

      {/* Row 4: Top Cost Drivers */}
      <Row style={{ marginBottom: 16 }}>
        <Col span={24}>
          <Card title="Top Cost Drivers">
            <Table
              dataSource={drivers?.items || []}
              columns={driverColumns}
              rowKey={(r) => `${r.market}-${r.serviceName}`}
              size="small"
              pagination={false}
            />
          </Card>
        </Col>
      </Row>

      {/* Row 5: Top Cost Movers */}
      <Row style={{ marginBottom: 16 }}>
        <Col span={24}>
          <Card title="Top Cost Movers">
            <Table
              dataSource={movers?.items || []}
              columns={moverColumns}
              rowKey={(r) => `${r.market}-${r.serviceName}`}
              size="small"
              pagination={false}
            />
          </Card>
        </Col>
      </Row>

      {/* Row 6: Budget vs Actual by Market */}
      <Row>
        <Col span={24}>
          <Card title="Budget vs Actual by Market">
            <Table
              dataSource={budgets?.items || []}
              columns={budgetColumns}
              rowKey="market"
              size="small"
              pagination={false}
            />
          </Card>
        </Col>
      </Row>
    </Spin>
  );
};

export default MarketOverviewPage;
