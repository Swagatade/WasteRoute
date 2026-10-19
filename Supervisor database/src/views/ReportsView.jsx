import React, { useState } from 'react';
import { 
  Download, 
  Printer, 
  TrendingUp, 
  Calendar, 
  CheckCircle2, 
  Clock, 
  AlertCircle, 
  FileSpreadsheet,
  Award,
  Filter
} from 'lucide-react';

import WorkerPhone from '../components/WorkerPhone';
import { workerPhone, assignedWorkerPhone } from '../utils/workerContact';

export default function ReportsView({ complaints, workers, bins }) {
  const [reportRange, setReportRange] = useState('7d');
  const [downloadNotice, setDownloadNotice] = useState(null);

  // Generate real CSV download
  const handleExportCSV = () => {
    const headers = ['Ticket No', 'Category', 'Priority', 'Status', 'Location', 'Ward', 'Citizen Name', 'Citizen Phone', 'Assigned Worker', 'Worker Mobile', 'Reported Time'];
    const rows = complaints.map(c => [
      c.ticketNo,
      `"${c.category}"`,
      c.priority,
      c.status,
      `"${c.location}"`,
      `"${c.wardName}"`,
      `"${c.citizen.name}"`,
      `"${c.citizen.phone}"`,
      `"${c.assignedWorkerName || 'Unassigned'}"`,
      `"${assignedWorkerPhone(workers, c)}"`,
      `"${c.reportedAt}"`
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map(e => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `WasteRoute_Supervisor_Audit_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    setDownloadNotice('Municipal Audit CSV successfully generated and downloaded!');
    setTimeout(() => setDownloadNotice(null), 4000);
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Header & Export Actions */}
      <div className="glass-panel" style={{ padding: '20px 24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '14px' }}>
          <div>
            <h2 style={{ fontSize: '20px', fontWeight: 800 }}>Task Monitoring, Reports & Analytics</h2>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
              Comprehensive SLA compliance, municipal waste collection trends, and downloadable audit logs.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '10px' }}>
            <button 
              onClick={handleExportCSV}
              className="btn btn-primary btn-sm"
              style={{ gap: '6px' }}
            >
              <Download size={15} />
              <span>Export CSV Audit</span>
            </button>
            <button 
              onClick={handlePrint}
              className="btn btn-secondary btn-sm"
              style={{ gap: '6px' }}
            >
              <Printer size={15} />
              <span>Print Brief</span>
            </button>
          </div>
        </div>

        {downloadNotice && (
          <div style={{ marginTop: '12px', padding: '10px 14px', borderRadius: '10px', background: 'rgba(16, 185, 129, 0.15)', border: '1px solid rgba(16, 185, 129, 0.4)', color: '#A7F3D0', fontSize: '13px' }}>
            📊 {downloadNotice}
          </div>
        )}
      </div>

      {/* SLA Metric Highlights */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px' }}>
        <div className="glass-panel" style={{ padding: '20px' }}>
          <span style={{ fontSize: '12.5px', color: 'var(--text-secondary)', fontWeight: 600 }}>SLA Compliance Rate</span>
          <div style={{ fontSize: '28px', fontWeight: 800, color: '#10B981', marginTop: '6px' }}>
            96.4%
          </div>
          <span style={{ fontSize: '11.5px', color: 'var(--text-muted)' }}>Target: &gt;95.0% within 4 hours</span>
        </div>

        <div className="glass-panel" style={{ padding: '20px' }}>
          <span style={{ fontSize: '12.5px', color: 'var(--text-secondary)', fontWeight: 600 }}>Average Response Time</span>
          <div style={{ fontSize: '28px', fontWeight: 800, color: '#06B6D4', marginTop: '6px' }}>
            1.8 Hours
          </div>
          <span style={{ fontSize: '11.5px', color: 'var(--text-muted)' }}>From citizen report to crew arrival</span>
        </div>

        <div className="glass-panel" style={{ padding: '20px' }}>
          <span style={{ fontSize: '12.5px', color: 'var(--text-secondary)', fontWeight: 600 }}>Citizen Satisfaction</span>
          <div style={{ fontSize: '28px', fontWeight: 800, color: '#FBBF24', marginTop: '6px' }}>
            4.85 / 5.0
          </div>
          <span style={{ fontSize: '11.5px', color: 'var(--text-muted)' }}>Based on 420+ rated cleanups</span>
        </div>

        <div className="glass-panel" style={{ padding: '20px' }}>
          <span style={{ fontSize: '12.5px', color: 'var(--text-secondary)', fontWeight: 600 }}>Total Waste Cleared (Today)</span>
          <div style={{ fontSize: '28px', fontWeight: 800, color: '#FFF', marginTop: '6px' }}>
            38.4 Tons
          </div>
          <span style={{ fontSize: '11.5px', color: 'var(--text-muted)' }}>Processed across 5 municipal depots</span>
        </div>
      </div>

      {/* Analytics Breakdown: Categories & Zone Performance */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '20px' }}>
        {/* Category Breakdown */}
        <div className="glass-panel" style={{ padding: '22px' }}>
          <h3 style={{ fontSize: '16.5px', fontWeight: 700, marginBottom: '6px' }}>Grievance Categories Distribution</h3>
          <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginBottom: '18px' }}>Proportion of reported waste anomalies</p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            {[
              { label: 'Overflowing Bins', pct: 44, color: '#EF4444' },
              { label: 'Illegal Dumping / Debris', pct: 26, color: '#F59E0B' },
              { label: 'Hazardous / Chemical Discards', pct: 14, color: '#8B5CF6' },
              { label: 'Missed Scheduled Route', pct: 10, color: '#06B6D4' },
              { label: 'Damaged Smart Bin Hardware', pct: 6, color: '#10B981' }
            ].map((cat, i) => (
              <div key={i}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', marginBottom: '5px' }}>
                  <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{cat.label}</span>
                  <span style={{ fontWeight: 700, color: cat.color }}>{cat.pct}%</span>
                </div>
                <div style={{ width: '100%', height: '8px', background: 'rgba(255, 255, 255, 0.08)', borderRadius: '4px', overflow: 'hidden' }}>
                  <div style={{ width: `${cat.pct}%`, height: '100%', background: cat.color, borderRadius: '4px' }} />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Fleet Performance Table */}
        <div className="glass-panel" style={{ padding: '22px' }}>
          <h3 style={{ fontSize: '16.5px', fontWeight: 700, marginBottom: '6px' }}>Sanitation Crew Leaderboard</h3>
          <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginBottom: '16px' }}>Top performing collection units today</p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {workers.length === 0 ? (
              <div style={{ padding: '28px 14px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '13px' }}>
                No registered workers to display on the leaderboard yet.
              </div>
            ) : (
              workers.map((w) => (
                <div 
                  key={w.id}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '10px 12px',
                    background: 'rgba(255, 255, 255, 0.03)',
                    borderRadius: '10px',
                    border: '1px solid var(--glass-border)'
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '13px', color: '#FFF' }}>{w.name}</div>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>{w.vehicle}</div>
                    <WorkerPhone phone={workerPhone(w)} size={11} style={{ fontSize: 11, marginTop: 2 }} />
                  </div>

                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: '13px', fontWeight: 800, color: '#34D399' }}>
                      {w.tasksCompletedToday} Cleared
                    </div>
                    <div style={{ fontSize: '11px', color: '#FBBF24' }}>
                      ★ {w.rating} Rating
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
