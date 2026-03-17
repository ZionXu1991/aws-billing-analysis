import React, { useCallback, useEffect, useState } from 'react';
import {
  Card,
  Table,
  Tag,
  Badge,
  Button,
  Modal,
  Form,
  Input,
  Select,
  Popconfirm,
  Space,
  Typography,
  message,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import {
  getAccounts,
  createAccount,
  updateAccount,
  deleteAccount,
} from '../services/mockData';
import type { AccountItem } from '../services/billingApi';

const envOptions = [
  { label: 'DEV', value: 'DEV' },
  { label: 'PREPROD', value: 'PREPROD' },
  { label: 'PROD', value: 'PROD' },
];

const marketOptions = [
  { label: 'US', value: 'US' },
  { label: 'UK', value: 'UK' },
  { label: 'DE', value: 'DE' },
  { label: 'FR', value: 'FR' },
  { label: 'JP', value: 'JP' },
  { label: 'SG', value: 'SG' },
  { label: 'AU', value: 'AU' },
  { label: 'BR', value: 'BR' },
  { label: 'IN', value: 'IN' },
  { label: 'CA', value: 'CA' },
];

const envColors: Record<string, string> = {
  DEV: 'blue',
  PREPROD: 'orange',
  PROD: 'red',
};

const BillingAccountsPage: React.FC = () => {
  const [data, setData] = useState<AccountItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editRecord, setEditRecord] = useState<AccountItem | null>(null);
  const [marketFilter, setMarketFilter] = useState<string | undefined>(undefined);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getAccounts();
      setData(result);
    } catch {
      message.error('Failed to load accounts');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const openAddModal = () => {
    setEditRecord(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEditModal = (record: AccountItem) => {
    setEditRecord(record);
    form.setFieldsValue({
      accountId: record.accountId,
      accountName: record.accountName,
      team: record.team,
      environment: record.environment,
      region: record.region,
      market: record.market,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editRecord) {
        await updateAccount(editRecord.id, values);
        message.success('Account updated');
      } else {
        await createAccount(values);
        message.success('Account created');
      }
      setModalOpen(false);
      fetchData();
    } catch {
      // validation or API error
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteAccount(id);
      message.success('Account deleted');
      fetchData();
    } catch {
      message.error('Failed to delete account');
    }
  };

  const columns: ColumnsType<AccountItem> = [
    { title: 'Account ID', dataIndex: 'accountId', key: 'accountId', width: 140 },
    { title: 'Name', dataIndex: 'accountName', key: 'accountName', width: 200 },
    { title: 'Team', dataIndex: 'team', key: 'team', width: 150 },
    { title: 'Market', dataIndex: 'market', key: 'market', width: 80 },
    {
      title: 'Environment',
      dataIndex: 'environment',
      key: 'environment',
      width: 120,
      render: (v: string) => <Tag color={envColors[v] || 'default'}>{v}</Tag>,
    },
    { title: 'Region', dataIndex: 'region', key: 'region', width: 140 },
    {
      title: 'Active',
      dataIndex: 'active',
      key: 'active',
      width: 80,
      render: (v: boolean) => (
        <Badge status={v ? 'success' : 'default'} text={v ? 'Yes' : 'No'} />
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
            title="Delete this account?"
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
          Accounts
        </Typography.Title>
        <Space>
          <Select
            value={marketFilter}
            onChange={setMarketFilter}
            options={[{ label: 'All Markets', value: undefined }, ...marketOptions]}
            placeholder="Filter by market"
            style={{ width: 160 }}
            allowClear
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={openAddModal}>
            Add Account
          </Button>
        </Space>
      </div>

      <Card>
        <Table
          dataSource={marketFilter ? data.filter((a) => a.market === marketFilter) : data}
          columns={columns}
          rowKey="id"
          loading={loading}
          size="small"
          pagination={{ pageSize: 10, showSizeChanger: true }}
        />
      </Card>

      <Modal
        title={editRecord ? 'Edit Account' : 'Add Account'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        okText={editRecord ? 'Update' : 'Create'}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item
            name="accountId"
            label="Account ID"
            rules={[{ required: true, message: 'Please enter Account ID' }]}
          >
            <Input placeholder="e.g. 123456789012" disabled={!!editRecord} />
          </Form.Item>
          <Form.Item
            name="accountName"
            label="Account Name"
            rules={[{ required: true, message: 'Please enter Account Name' }]}
          >
            <Input placeholder="e.g. Production Main" />
          </Form.Item>
          <Form.Item
            name="team"
            label="Team"
            rules={[{ required: true, message: 'Please enter Team' }]}
          >
            <Input placeholder="e.g. Platform Engineering" />
          </Form.Item>
          <Form.Item
            name="environment"
            label="Environment"
            rules={[{ required: true, message: 'Please select Environment' }]}
          >
            <Select options={envOptions} placeholder="Select environment" />
          </Form.Item>
          <Form.Item
            name="market"
            label="Market"
            rules={[{ required: true, message: 'Please select Market' }]}
          >
            <Select options={marketOptions} placeholder="Select market" />
          </Form.Item>
          <Form.Item
            name="region"
            label="Region"
            rules={[{ required: true, message: 'Please enter Region' }]}
          >
            <Input placeholder="e.g. us-east-1" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default BillingAccountsPage;
