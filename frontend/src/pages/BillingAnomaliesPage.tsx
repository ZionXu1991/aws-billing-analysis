import React, { useCallback, useEffect, useState } from 'react';
import {
  Card,
  Table,
  Tag,
  Badge,
  Select,
  DatePicker,
  Button,
  Modal,
  Row,
  Col,
  Space,
  Typography,
  message,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import dayjs from 'dayjs';
import {
  getAnomalies,
  acknowledgeAnomaly,
} from '../services/mockData';
import type { AnomalyItem } from '../services/billingApi';

const { RangePicker } = DatePicker;

const severityOptions = [
  { label: 'All Severities', value: '' },
  { label: 'CRITICAL', value: 'CRITICAL' },
  { label: 'HIGH', value: 'HIGH' },
  { label: 'MEDIUM', value: 'MEDIUM' },
  { label: 'LOW', value: 'LOW' },
];

const statusOptions = [
  { label: 'All Statuses', value: '' },
  { label: 'OPEN', value: 'OPEN' },
  { label: 'ACKNOWLEDGED', value: 'ACKNOWLEDGED' },
  { label: 'RESOLVED', value: 'RESOLVED' },
];

const severityColors: Record<string, string> = {
  CRITICAL: 'red',
  HIGH: 'orange',
  MEDIUM: 'gold',
  LOW: 'blue',
};

const statusBadge: Record<string, 'error' | 'warning' | 'success' | 'default'> = {
  OPEN: 'error',
  ACKNOWLEDGED: 'warning',
  RESOLVED: 'success',
};

const formatCost = (value: number): string =>
  `$${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

const BillingAnomaliesPage: React.FC = () => {
  const [data, setData] = useState<AnomalyItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [severity, setSeverity] = useState('');
  const [status, setStatus] = useState('');
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs | null, dayjs.Dayjs | null] | null>(null);
  const [ackId, setAckId] = useState<number | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string | number> = { page, size: pageSize };
      if (severity) params.severity = severity;
      if (status) params.status = status;
      if (dateRange?.[0]) params.startDate = dateRange[0].format('YYYY-MM-DD');
      if (dateRange?.[1]) params.endDate = dateRange[1].format('YYYY-MM-DD');

      const result = await getAnomalies(params);
      setData(result.content);
      setTotal(result.totalElements);
    } catch {
      message.error('Failed to load anomalies');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, severity, status, dateRange]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleTableChange = (pagination: TablePaginationConfig) => {
    setPage((pagination.current ?? 1) - 1);
    setPageSize(pagination.pageSize ?? 10);
  };

  const handleAcknowledge = async () => {
    if (ackId === null) return;
    try {
      await acknowledgeAnomaly(ackId);
      message.success('Anomaly acknowledged');
      setAckId(null);
      fetchData();
    } catch {
      message.error('Failed to acknowledge anomaly');
    }
  };

  const columns: ColumnsType<AnomalyItem> = [
    {
      title: 'Detected Date',
      dataIndex: 'detectedDate',
      key: 'detectedDate',
      width: 120,
    },
    {
      title: 'Account',
      key: 'account',
      render: (_, r) => <span>{r.accountName} ({r.accountId})</span>,
      width: 200,
    },
    {
      title: 'Service',
      dataIndex: 'serviceName',
      key: 'serviceName',
      width: 160,
    },
    {
      title: 'Type',
      dataIndex: 'anomalyType',
      key: 'anomalyType',
      render: (v: string) => <Tag>{v}</Tag>,
      width: 120,
    },
    {
      title: 'Severity',
      dataIndex: 'severity',
      key: 'severity',
      render: (v: string) => <Tag color={severityColors[v]}>{v}</Tag>,
      width: 100,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (v: string) => <Badge status={statusBadge[v] || 'default'} text={v} />,
      width: 130,
    },
    {
      title: 'Expected Cost',
      dataIndex: 'expectedCost',
      key: 'expectedCost',
      render: (v: number) => formatCost(v),
      width: 130,
      align: 'right',
    },
    {
      title: 'Actual Cost',
      dataIndex: 'actualCost',
      key: 'actualCost',
      render: (v: number) => formatCost(v),
      width: 130,
      align: 'right',
    },
    {
      title: 'Deviation%',
      dataIndex: 'deviationPercent',
      key: 'deviationPercent',
      render: (v: number) => <span style={{ color: '#f5222d' }}>{v.toFixed(1)}%</span>,
      width: 100,
      align: 'right',
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 120,
      render: (_, record) =>
        record.status === 'OPEN' ? (
          <Button type="link" size="small" onClick={() => setAckId(record.id)}>
            Acknowledge
          </Button>
        ) : null,
    },
  ];

  return (
    <div>
      <Typography.Title level={3} style={{ marginBottom: 16 }}>
        Anomalies
      </Typography.Title>

      <Card style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col>
            <Space>
              <Select
                value={severity}
                onChange={(v) => { setSeverity(v); setPage(0); }}
                options={severityOptions}
                style={{ width: 160 }}
                placeholder="Severity"
              />
              <Select
                value={status}
                onChange={(v) => { setStatus(v); setPage(0); }}
                options={statusOptions}
                style={{ width: 160 }}
                placeholder="Status"
              />
              <RangePicker
                onChange={(dates) => {
                  setDateRange(dates as [dayjs.Dayjs | null, dayjs.Dayjs | null] | null);
                  setPage(0);
                }}
              />
            </Space>
          </Col>
        </Row>
      </Card>

      <Card>
        <Table
          dataSource={data}
          columns={columns}
          rowKey="id"
          loading={loading}
          size="small"
          scroll={{ x: 1300 }}
          pagination={{
            current: page + 1,
            pageSize,
            total,
            showSizeChanger: true,
            showTotal: (t) => `Total ${t} anomalies`,
          }}
          onChange={handleTableChange}
        />
      </Card>

      <Modal
        title="Acknowledge Anomaly"
        open={ackId !== null}
        onOk={handleAcknowledge}
        onCancel={() => setAckId(null)}
        okText="Confirm"
      >
        <p>Are you sure you want to acknowledge this anomaly?</p>
      </Modal>
    </div>
  );
};

export default BillingAnomaliesPage;
