import React, { useState } from 'react';
import { Select, Button, Tag, Space } from 'antd';
import { Area, Column, Pie } from '@ant-design/charts';
import { 
  CaretUpOutlined, CaretDownOutlined, RocketOutlined, 
  BulbOutlined, GlobalOutlined, CloudServerOutlined, 
  ThunderboltOutlined, SettingOutlined 
} from '@ant-design/icons';
import './DevOpsPlatformCostDashboard.css';

const { Option } = Select;

// --- MOCK DATA ---
const trendData = [
  ...Array.from({length: 6}).map((_, i) => ({ month: `2025-0${i+1}`, category: 'Production', value: 120000 + Math.random() * 40000 })),
  ...Array.from({length: 6}).map((_, i) => ({ month: `2025-0${i+1}`, category: 'PreProd', value: 30000 + Math.random() * 15000 })),
  ...Array.from({length: 6}).map((_, i) => ({ month: `2025-0${i+1}`, category: 'Development', value: 45000 + Math.random() * 20000 }))
];

const serviceData = [
  { type: 'Amazon EC2', value: 450000 },
  { type: 'Amazon RDS', value: 220000 },
  { type: 'Amazon S3', value: 85000 },
  { type: 'Amazon EKS', value: 180000 },
  { type: 'AWS Lambda', value: 65000 },
  { type: 'Others', value: 92000 },
];

const marketDataRaw = [
  { market: 'US', dev: 25000, preprod: 15000, prod: 110000 },
  { market: 'UK', dev: 18000, preprod: 12000, prod: 85000 },
  { market: 'Japan', dev: 15000, preprod: 10000, prod: 60000 },
  { market: 'Germany', dev: 12000, preprod: 8000, prod: 55000 },
  { market: 'India', dev: 22000, preprod: 14000, prod: 45000 },
  { market: 'Australia', dev: 10000, preprod: 6000, prod: 35000 },
];

const marketData = marketDataRaw.flatMap(o => [
  { market: o.market, environment: 'Dev', cost: o.dev },
  { market: o.market, environment: 'PreProd', cost: o.preprod },
  { market: o.market, environment: 'Prod', cost: o.prod }
]);

const accountData = Array.from({length: 10}).map((_, i) => ({
  account: `Account-${i + 1}`,
  cost: Math.floor(20000 + Math.random() * 80000)
})).sort((a, b) => b.cost - a.cost);

// --- COMPONENT ---
const DevOpsPlatformCostDashboard: React.FC = () => {
  const [marketFilter, setMarketFilter] = useState('all');

  // Chart configs using @ant-design/charts v2 syntax
  const areaConfig = {
    data: trendData,
    xField: 'month',
    yField: 'value',
    colorField: 'category',
    stack: true,
    color: ['#00f2fe', '#f093fb', '#43e97b'],
    style: { fillOpacity: 0.5 },
    legend: { position: 'top' as const, itemName: { style: { fill: '#fff' } } },
    axis: {
      x: { label: { style: { fill: '#aeb9e1' } } },
      y: { label: { style: { fill: '#aeb9e1' } }, grid: { line: { style: { stroke: 'rgba(255,255,255,0.05)' } } } }
    }
  };

  const servicePieConfig = {
    data: serviceData,
    angleField: 'value',
    colorField: 'type',
    innerRadius: 0.64,
    color: ['#00f2fe', '#4facfe', '#fa709a', '#fee140', '#f093fb', '#43e97b'],
    legend: { position: 'right' as const, itemName: { style: { fill: '#fff' } } },
    label: { text: 'value', style: { fill: '#fff', fontSize: 11, fontWeight: 'bold' } },
  };

  const marketColumnConfig = {
    data: marketData,
    xField: 'market',
    yField: 'cost',
    colorField: 'environment',
    stack: true,
    color: ['#00f2fe', '#f093fb', '#fee140'],
    legend: { position: 'top' as const, itemName: { style: { fill: '#fff' } } },
    axis: {
      x: { label: { style: { fill: '#aeb9e1' } } },
      y: { label: { style: { fill: '#aeb9e1' } }, grid: { line: { style: { stroke: 'rgba(255,255,255,0.05)' } } } }
    }
  };

  const accountColumnConfig = {
    data: accountData,
    xField: 'account',
    yField: 'cost',
    color: 'l(90) 0:#fa709a 1:#fee140',
    axis: {
      x: { label: { autoRotate: true, style: { fill: '#aeb9e1' } } },
      y: { label: { style: { fill: '#aeb9e1' } }, grid: { line: { stroke: 'rgba(255,255,255,0.05)' } } }
    }
  };

  return (
    <div className="devops-cost-container">
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div>
          <h1 style={{ color: '#fff', margin: 0, fontSize: 28, fontWeight: 700 }}>
            <span className="live-indicator"></span> 
            Platform Cost Intelligence
          </h1>
          <p style={{ color: '#aeb9e1', margin: '4px 0 0 0' }}>Multi-Market AI Cost Analytics Engine</p>
        </div>
        <Space>
          <Select value={marketFilter} onChange={setMarketFilter} style={{ width: 160 }} className="dark-select">
            <Option value="all">Global Markets (10)</Option>
            <Option value="us">US Market</Option>
            <Option value="uk">UK Market</Option>
            <Option value="jp">Japan Market</Option>
          </Select>
          <Select defaultValue="30d" style={{ width: 130 }} className="dark-select">
            <Option value="7d">Last 7 Days</Option>
            <Option value="30d">Last 30 Days</Option>
            <Option value="ytd">Year to Date</Option>
          </Select>
          <Button type="primary" style={{ background: 'linear-gradient(90deg, #f093fb 0%, #f5576c 100%)', border: 'none', borderRadius: 8, height: 32 }}>
            <CloudServerOutlined /> Generate Report
          </Button>
        </Space>
      </div>

      <div className="grid-layout" style={{ marginTop: 0 }}>
        {/* Top Stats */}
        <div className="col-span-3">
          <div className="glass-card">
            <div className="stat-title">Total Monthly Spend</div>
            <div className="stat-value">$1,092,000</div>
            <div style={{ color: '#aeb9e1', fontSize: 13, marginTop: 12 }}>
              <span className="trend-down"><CaretDownOutlined /> 2.1%</span> vs last month
            </div>
            <div style={{ marginTop: 16 }}>
              <Tag color="cyan" style={{ background: 'rgba(23,125,220,0.2)', border: '1px solid #177ddc' }}>30 Active Accounts</Tag>
            </div>
          </div>
        </div>

        <div className="col-span-3">
          <div className="glass-card">
            <div className="stat-title">Environment Burn Rate</div>
            <div className="stat-value green">$36,400 <span style={{fontSize:16, color:'#aeb9e1'}}>/ day</span></div>
            <div style={{ display: 'flex', justifyContent: 'space-between', color: '#aeb9e1', fontSize: 12, marginTop: 12 }}>
              <span>Prod: 70%</span>
              <span>Pre: 18%</span>
              <span>Dev: 12%</span>
            </div>
            <div style={{ width: '100%', height: 4, background: 'rgba(255,255,255,0.1)', borderRadius: 2, marginTop: 8, display: 'flex' }}>
              <div style={{ width: '70%', background: '#43e97b', borderRadius: '2px 0 0 2px' }}></div>
              <div style={{ width: '18%', background: '#f093fb' }}></div>
              <div style={{ width: '12%', background: '#00f2fe', borderRadius: '0 2px 2px 0' }}></div>
            </div>
          </div>
        </div>

        <div className="col-span-3">
          <div className="glass-card">
            <div className="stat-title">AI Optimization Gap</div>
            <div className="stat-value orange">$142,800</div>
            <div style={{ color: '#aeb9e1', fontSize: 13, marginTop: 12 }}>
              Potential savings found across <span style={{color: '#fff', fontWeight: 'bold'}}>15</span> services
            </div>
            <Button size="small" style={{ marginTop: 12, background: 'rgba(254,225,64,0.1)', color: '#fee140', border: '1px solid #fee140', borderRadius: 12 }}>
              <BulbOutlined /> Review 6 Opportunities
            </Button>
          </div>
        </div>

        <div className="col-span-3">
          <div className="glass-card">
            <div className="chart-header">
               <span style={{fontSize: 15}}><GlobalOutlined style={{marginRight: 8}}/> Top Resourced Market</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'flex-end', gap: 12 }}>
              <span className="stat-value purple" style={{fontSize: 28}}>US ($485k)</span>
              <span className="trend-up" style={{marginBottom: 8}}><CaretUpOutlined /> 4.5%</span>
            </div>
            <div style={{ color: '#aeb9e1', fontSize: 12, marginTop: 16 }}>Region: us-east-1 leading spend</div>
            <div style={{ color: '#aeb9e1', fontSize: 12, marginTop: 4 }}>Accounts: 8 dedicated accounts</div>
          </div>
        </div>

        {/* Charts Row 1 */}
        <div className="col-span-8">
          <div className="glass-card" style={{ minHeight: 340 }}>
            <div className="chart-header">Spend Trend by Environment (YTD)</div>
            <div style={{ height: 280 }}>
              <Area {...areaConfig} />
            </div>
          </div>
        </div>

        <div className="col-span-4">
          <div className="glass-card" style={{ minHeight: 340 }}>
            <div className="chart-header">Cost by AWS Services</div>
            <div style={{ height: 280 }}>
              <Pie {...servicePieConfig} />
            </div>
          </div>
        </div>

        {/* Charts Row 2 */}
        <div className="col-span-8">
          <div className="glass-card" style={{ minHeight: 340 }}>
            <div className="chart-header">Market & Environment Comparison</div>
            <div style={{ height: 280 }}>
              <Column {...marketColumnConfig} />
            </div>
          </div>
        </div>

        {/* AI Recommendations */}
        <div className="col-span-4">
          <div className="glass-card" style={{ minHeight: 340 }}>
            <div className="chart-header"><RocketOutlined style={{marginRight: 8, color: '#f5576c'}}/> AI Suggestions</div>
            <ul className="recommendation-list">
              <li className="recommendation-item">
                <div className="recommend-icon"><BulbOutlined /></div>
                <div className="recommend-content">
                  <h4>RDS Right-sizing in PreProd</h4>
                  <p>3 clusters in Japan market have max 15% CPU utilization.</p>
                  <div className="recommend-action">
                    <Button size="small">Save ~$4,200/mo</Button>
                  </div>
                </div>
              </li>
              <li className="recommendation-item">
                <div className="recommend-icon" style={{color: '#00f2fe'}}><ThunderboltOutlined /></div>
                <div className="recommend-content">
                  <h4>EC2 Spot Instances for Dev</h4>
                  <p>Convert on-demand EKS nodes in US Market to Spot.</p>
                  <div className="recommend-action">
                    <Button size="small">Save ~$12,500/mo</Button>
                  </div>
                </div>
              </li>
              <li className="recommendation-item" style={{borderBottom: 'none'}}>
                 <div className="recommend-icon" style={{color: '#fee140'}}><SettingOutlined /></div>
                <div className="recommend-content">
                  <h4>S3 Intelligent Tiering</h4>
                  <p>Enable for bucket 'assets-uk-prod-01' (102TB).</p>
                  <div className="recommend-action">
                    <Button size="small">Save ~$1,800/mo</Button>
                  </div>
                </div>
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom Accounts Bar */}
        <div className="col-span-12">
          <div className="glass-card" style={{ minHeight: 300 }}>
            <div className="chart-header">Top 10 High-Cost Accounts</div>
            <div style={{ height: 240 }}>
              <Column {...accountColumnConfig} />
            </div>
          </div>
        </div>

      </div>
    </div>
  );
};

export default DevOpsPlatformCostDashboard;
