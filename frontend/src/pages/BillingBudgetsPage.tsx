import React, { useCallback, useEffect, useState } from 'react';
import {
  Card,
  Table,
  Button,
  Modal,
  Form,
  Input,
  InputNumber,
  Select,
  Popconfirm,
  Progress,
  Space,
  Typography,
  message,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
  getBudgets,
  createBudget,
  updateBudget,
  deleteBudget,
} from '../services/mockData';
import type { BudgetItem } from '../services/billingApi';

const formatCost = (value: number): string =>
  `$${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

const generateMonthOptions = () => {
  const options: { label: string; value: string }[] = [];
  const now = dayjs();
  for (let i = -3; i < 12; i++) {
    const d = now.subtract(i, 'month');
    options.push({ label: d.format('YYYY-MM'), value: d.format('YYYY-MM') });
  }
  return options;
};

const getProgressColor = (pct: number): string => {
  if (pct > 100) return '#f5222d';
  if (pct >= 80) return '#faad14';
  return '#52c41a';
};

const BillingBudgetsPage: React.FC = () => {
  const [yearMonth, setYearMonth] = useState(dayjs().format('YYYY-MM'));
  const [data, setData] = useState<BudgetItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editRecord, setEditRecord] = useState<BudgetItem | null>(null);
  const [form] = Form.useForm();

  const fetchData = useCallback(async (ym: string) => {
    setLoading(true);
    try {
      const result = await getBudgets(ym);
      setData(result);
    } catch {
      message.error('Failed to load budgets');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData(yearMonth);
  }, [yearMonth, fetchData]);

  const openAddModal = () => {
    setEditRecord(null);
    form.resetFields();
    form.setFieldsValue({ yearMonth });
    setModalOpen(true);
  };

  const openEditModal = (record: BudgetItem) => {
    setEditRecord(record);
    form.setFieldsValue({
      accountId: record.accountId || '',
      serviceName: record.serviceName || '',
      yearMonth: record.yearMonth,
      budgetAmount: record.budgetAmount,
      alertThresholdPercent: record.alertThresholdPercent,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const payload = {
        ...values,
        accountId: values.accountId || null,
        serviceName: values.serviceName || null,
      };
      if (editRecord) {
        await updateBudget(editRecord.id, payload);
        message.success('Budget updated');
      } else {
        await createBudget(payload);
        message.success('Budget created');
      }
      setModalOpen(false);
      fetchData(yearMonth);
    } catch {
      // validation or API error
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteBudget(id);
      message.success('Budget deleted');
      fetchData(yearMonth);
    } catch {
      message.error('Failed to delete budget');
    }
  };

  const getScope = (record: BudgetItem): string => {
    if (record.accountId && record.serviceName) return `${record.accountId} / ${record.serviceName}`;
    if (record.accountId) return record.accountId;
    if (record.serviceName) return record.serviceName;
    return 'All';
  };

  const columns: ColumnsType<BudgetItem> = [
    {
      title: 'Scope',
      key: 'scope',
      render: (_, r) => getScope(r),
      width: 250,
    },
    { title: 'Year-Month', dataIndex: 'yearMonth', key: 'yearMonth', width: 110 },
    {
      title: 'Budget',
      dataIndex: 'budgetAmount',
      key: 'budgetAmount',
      render: (v: number) => formatCost(v),
      width: 130,
      align: 'right',
    },
    {
      title: 'Actual',
      dataIndex: 'actualAmount',
      key: 'actualAmount',
      render: (v: number) => formatCost(v),
      width: 130,
      align: 'right',
    },
    {
      title: 'Utilization',
      dataIndex: 'utilizationPercent',
      key: 'utilizationPercent',
      width: 200,
      render: (v: number) => (
        <Progress
          percent={Math.min(v, 150)}
          format={() => `${v.toFixed(1)}%`}
          strokeColor={getProgressColor(v)}
          size="small"
        />
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 120,
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => openEditModal(record)}
          />
          <Popconfirm
            title="Delete this budget?"
            onConfirm={() => handleDelete(record.id)}
            okText="Yes"
            cancelText="No"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          Budgets
        </Typography.Title>
        <Space>
          <Select
            value={yearMonth}
            onChange={setYearMonth}
            options={generateMonthOptions()}
            style={{ width: 160 }}
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={openAddModal}>
            Add Budget
          </Button>
        </Space>
      </div>

      <Card>
        <Table
          dataSource={data}
          columns={columns}
          rowKey="id"
          loading={loading}
          size="small"
          pagination={{ pageSize: 10, showSizeChanger: true }}
        />
      </Card>

      <Modal
        title={editRecord ? 'Edit Budget' : 'Add Budget'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        okText={editRecord ? 'Update' : 'Create'}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="accountId" label="Account ID (optional)">
            <Input placeholder="Leave empty for all accounts" />
          </Form.Item>
          <Form.Item name="serviceName" label="Service Name (optional)">
            <Input placeholder="Leave empty for all services" />
          </Form.Item>
          <Form.Item
            name="yearMonth"
            label="Year-Month"
            rules={[{ required: true, message: 'Please select Year-Month' }]}
          >
            <Select options={generateMonthOptions()} placeholder="Select month" />
          </Form.Item>
          <Form.Item
            name="budgetAmount"
            label="Budget Amount ($)"
            rules={[{ required: true, message: 'Please enter budget amount' }]}
          >
            <InputNumber
              min={0}
              step={100}
              style={{ width: '100%' }}
              formatter={(value) => `$ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={(value) => Number(value?.replace(/\$\s?|(,*)/g, '') ?? 0) as unknown as 0}
            />
          </Form.Item>
          <Form.Item
            name="alertThresholdPercent"
            label="Alert Threshold (%)"
            rules={[{ required: true, message: 'Please enter alert threshold' }]}
          >
            <InputNumber min={0} max={200} step={5} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default BillingBudgetsPage;
