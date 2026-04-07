import React from 'react';
import { Routes, Route, useNavigate, useLocation, Navigate } from 'react-router-dom';
import { ConfigProvider, Layout, Menu, theme, Typography } from 'antd';
import {
  DollarOutlined,
  AlertOutlined,
  TeamOutlined,
  FundOutlined,
  GlobalOutlined,
  BarChartOutlined,
} from '@ant-design/icons';
import BillingDashboardPage from './pages/BillingDashboardPage';
import BillingAnomaliesPage from './pages/BillingAnomaliesPage';
import BillingAccountsPage from './pages/BillingAccountsPage';
import BillingBudgetsPage from './pages/BillingBudgetsPage';
import MarketOverviewPage from './pages/MarketOverviewPage';
import CostAnalysisPage from './pages/CostAnalysisPage';
import DevOpsPlatformCostDashboard from './pages/DevOpsPlatformCostDashboard';

const { Sider, Content } = Layout;
const { Title } = Typography;

const menuItems = [
  {
    key: '/billing/platform-intel',
    icon: <BarChartOutlined />,
    label: 'Platform Intel',
  },
  {
    key: '/billing/dashboard',
    icon: <DollarOutlined />,
    label: 'Dashboard',
  },
  {
    key: '/billing/markets',
    icon: <GlobalOutlined />,
    label: 'Markets',
  },
  {
    key: '/billing/analysis',
    icon: <BarChartOutlined />,
    label: 'Cost Analysis',
  },
  {
    key: '/billing/anomalies',
    icon: <AlertOutlined />,
    label: 'Anomalies',
  },
  {
    key: '/billing/accounts',
    icon: <TeamOutlined />,
    label: 'Accounts',
  },
  {
    key: '/billing/budgets',
    icon: <FundOutlined />,
    label: 'Budgets',
  },
];

const App: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <ConfigProvider theme={{ algorithm: theme.defaultAlgorithm }}>
      <Layout style={{ minHeight: '100vh' }}>
        <Sider width={240} theme="dark">
          <div
            style={{
              height: 64,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              borderBottom: '1px solid rgba(255,255,255,0.1)',
            }}
          >
            <Title level={4} style={{ color: '#fff', margin: 0 }}>
              AWS Billing
            </Title>
          </div>
          <Menu
            theme="dark"
            mode="inline"
            selectedKeys={[location.pathname]}
            items={menuItems}
            onClick={({ key }) => navigate(key)}
          />
        </Sider>
        <Layout>
          <Content style={{ margin: 24, minHeight: 280 }}>
            <Routes>
              <Route path="/billing/platform-intel" element={<DevOpsPlatformCostDashboard />} />
              <Route path="/billing/dashboard" element={<BillingDashboardPage />} />
              <Route path="/billing/markets" element={<MarketOverviewPage />} />
              <Route path="/billing/analysis" element={<CostAnalysisPage />} />
              <Route path="/billing/anomalies" element={<BillingAnomaliesPage />} />
              <Route path="/billing/accounts" element={<BillingAccountsPage />} />
              <Route path="/billing/budgets" element={<BillingBudgetsPage />} />
              <Route path="*" element={<Navigate to="/billing/platform-intel" replace />} />
            </Routes>
          </Content>
        </Layout>
      </Layout>
    </ConfigProvider>
  );
};

export default App;
