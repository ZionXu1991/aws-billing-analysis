import React, { useCallback, useEffect, useState } from 'react';
import { Row, Col, Card, Select, Spin, Table, Button, Typography, Space, message } from 'antd';
import { BarChartOutlined, DownloadOutlined, ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';
import { Bar } from '@ant-design/charts';
import dayjs from 'dayjs';
import type { ColumnsType } from 'antd/es/table';
import { analyzeCosts, getFilterOptions } from '../services/mockData';
import type { CostAnalysisItem, CostAnalysisResponse, FilterOptionsResponse, CostAnalysisRequest } from '../services/billingApi';

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

const groupByOptions = [
  { label: 'Market', value: 'market' },
  { label: 'Service', value: 'service' },
  { label: 'Account', value: 'account' },
  { label: 'Environment', value: 'environment' },
];

const CostAnalysisPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [filterOptions, setFilterOptions] = useState<FilterOptionsResponse | null>(null);
  const [result, setResult] = useState<CostAnalysisResponse | null>(null);

  // Filter state
  const [selectedMarkets, setSelectedMarkets] = useState<string[]>([]);
  const [selectedEnvironments, setSelectedEnvironments] = useState<string[]>([]);
  const [selectedServices, setSelectedServices] = useState<string[]>([]);
  const [groupBy, setGroupBy] = useState('market');
  const [yearMonth, setYearMonth] = useState(dayjs().format('YYYY-MM'));

  const fetchFilters = useCallback(async () => {
    try {
      const opts = await getFilterOptions();
      setFilterOptions(opts);
    } catch {
      message.error('Failed to load filter options');
    }
  }, []);

  useEffect(() => {
    fetchFilters();
  }, [fetchFilters]);

  const runAnalysis = useCallback(async () => {
    setLoading(true);
    try {
      const request: CostAnalysisRequest = {
        groupBy,
        startYearMonth: yearMonth,
        endYearMonth: yearMonth,
      };
      if (selectedMarkets.length > 0) request.markets = selectedMarkets;
      if (selectedEnvironments.length > 0) request.environments = selectedEnvironments;
      if (selectedServices.length > 0) request.services = selectedServices;
      const res = await analyzeCosts(request);
      setResult(res);
    } catch {
      message.error('Failed to run analysis');
    } finally {
      setLoading(false);
    }
  }, [groupBy, yearMonth, selectedMarkets, selectedEnvironments, selectedServices]);

  useEffect(() => {
    runAnalysis();
  }, [runAnalysis]);

  const exportCsv = () => {
    if (!result) return;
    const header = 'Group,Cost,Percentage,MoM Change%';
    const rows = result.items.map(
      (item) => `"${item.group}",${item.cost},${item.percentage},${item.momChangePercent}`,
    );
    const csv = [header, ...rows].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `cost-analysis-${yearMonth}-${groupBy}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  };

  const barData = result
    ? [...result.items].reverse().map((item) => ({
        group: item.group,
        cost: item.cost,
      }))
    : [];

  const barConfig = {
    data: barData,
    xField: 'cost',
    yField: 'group',
    color: '#1890ff',
    label: {
      position: 'right' as const,
      formatter: (datum: { cost?: number }) => formatCost(datum.cost ?? 0),
    },
  };

  const columns: ColumnsType<CostAnalysisItem> = [
    { title: 'Group', dataIndex: 'group', key: 'group', sorter: (a, b) => a.group.localeCompare(b.group) },
    {
      title: 'Cost',
      dataIndex: 'cost',
      key: 'cost',
      render: (v: number) => formatCost(v),
      sorter: (a, b) => a.cost - b.cost,
      defaultSortOrder: 'descend',
    },
    {
      title: 'Percentage',
      dataIndex: 'percentage',
      key: 'percentage',
      render: (v: number) => `${v.toFixed(1)}%`,
      sorter: (a, b) => a.percentage - b.percentage,
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

  return (
    <Spin spinning={loading}>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          <BarChartOutlined style={{ marginRight: 8 }} />
          Cost Analysis
        </Typography.Title>
      </div>

      {/* Filter Bar */}
      <Card style={{ marginBottom: 16 }}>
        <Space wrap size="middle">
          <div>
            <div style={{ marginBottom: 4, fontSize: 12, color: '#999' }}>Month</div>
            <Select
              value={yearMonth}
              onChange={setYearMonth}
              options={generateMonthOptions()}
              style={{ width: 140 }}
            />
          </div>
          <div>
            <div style={{ marginBottom: 4, fontSize: 12, color: '#999' }}>Group By</div>
            <Select
              value={groupBy}
              onChange={setGroupBy}
              options={groupByOptions}
              style={{ width: 140 }}
            />
          </div>
          <div>
            <div style={{ marginBottom: 4, fontSize: 12, color: '#999' }}>Markets</div>
            <Select
              mode="multiple"
              value={selectedMarkets}
              onChange={setSelectedMarkets}
              options={(filterOptions?.markets || []).map((m) => ({ label: m, value: m }))}
              placeholder="All markets"
              style={{ minWidth: 180 }}
              allowClear
              maxTagCount={2}
            />
          </div>
          <div>
            <div style={{ marginBottom: 4, fontSize: 12, color: '#999' }}>Environments</div>
            <Select
              mode="multiple"
              value={selectedEnvironments}
              onChange={setSelectedEnvironments}
              options={(filterOptions?.environments || []).map((e) => ({ label: e, value: e }))}
              placeholder="All environments"
              style={{ minWidth: 180 }}
              allowClear
              maxTagCount={2}
            />
          </div>
          <div>
            <div style={{ marginBottom: 4, fontSize: 12, color: '#999' }}>Services</div>
            <Select
              mode="multiple"
              value={selectedServices}
              onChange={setSelectedServices}
              options={(filterOptions?.services || []).map((s) => ({ label: s, value: s }))}
              placeholder="All services"
              style={{ minWidth: 220 }}
              allowClear
              maxTagCount={2}
            />
          </div>
        </Space>
      </Card>

      {/* Chart */}
      <Row style={{ marginBottom: 16 }}>
        <Col span={24}>
          <Card
            title={`Cost by ${groupBy.charAt(0).toUpperCase() + groupBy.slice(1)}`}
            extra={
              <Button icon={<DownloadOutlined />} onClick={exportCsv} disabled={!result}>
                Export CSV
              </Button>
            }
          >
            {barData.length > 0 && <Bar {...barConfig} height={350} />}
          </Card>
        </Col>
      </Row>

      {/* Results Table */}
      <Row>
        <Col span={24}>
          <Card
            title="Analysis Results"
            extra={
              result ? (
                <span style={{ color: '#999' }}>
                  Total: {formatCost(result.totalCost)}
                </span>
              ) : null
            }
          >
            <Table
              dataSource={result?.items || []}
              columns={columns}
              rowKey="group"
              size="small"
              pagination={{ pageSize: 15, showSizeChanger: true }}
            />
          </Card>
        </Col>
      </Row>
    </Spin>
  );
};

export default CostAnalysisPage;
