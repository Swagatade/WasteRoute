import React, { useState, useEffect, useRef } from 'react';
import { 
  MapPin, 
  Trash2, 
  Truck, 
  AlertTriangle, 
  Layers, 
  Radio, 
  Maximize2, 
  CheckCircle2, 
  X,
  Compass,
  Navigation,
  RefreshCw,
  ExternalLink,
  Home,
  Plus
} from 'lucide-react';
import StatusBadge from '../components/StatusBadge';
import { DEFAULT_ZONES } from '../data/mockData';
import { toDateString } from '../firebase/firestoreService';

const CAMPUS_DEFAULT_LAT = parseFloat(import.meta.env.VITE_CAMPUS_LAT) || 12.8230;
const CAMPUS_DEFAULT_LNG = parseFloat(import.meta.env.VITE_CAMPUS_LNG) || 80.0440;
const CAMPUS_DEFAULT_ZOOM = parseInt(import.meta.env.VITE_CAMPUS_ZOOM) || 16;
const OSM_TILE_URL = import.meta.env.VITE_OSM_TILE_URL || 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
const CAMPUS_NAME = import.meta.env.VITE_CAMPUS_NAME || 'SRM IST Kattankulathur (SRM KTR)';

// SRM KTR Campus Points of Interest for Quick Navigation
const CAMPUS_POIS = [
  { name: 'Campus Center', lat: 12.8230, lng: 80.0440, zoom: 16 },
  { name: 'UB Building', lat: 12.8232, lng: 80.0445, zoom: 18 },
  { name: 'Tech Park Tower', lat: 12.8248, lng: 80.0456, zoom: 18 },
  { name: 'Java Green Food St', lat: 12.8225, lng: 80.0438, zoom: 18 },
  { name: 'Paari / Kaari Hostels', lat: 12.8210, lng: 80.0415, zoom: 18 },
  { name: 'Main Gate 1 (GST Rd)', lat: 12.8258, lng: 80.0470, zoom: 18 },
  { name: 'Medical Hospital', lat: 12.8202, lng: 80.0485, zoom: 18 }
];

export default function MapView({ 
  bins = [], 
  complaints = [], 
  workers = [], 
  schedules = [],
  households = [],
  zones = [],
  onAddHostel,
  onSelectComplaint 
}) {
  const [filterType, setFilterType] = useState('all'); // all | bins | hostels
  // Add-hostel mode: click the map to drop a pin, name it, save to Firestore (same collection the workers read)
  const [addMode, setAddMode] = useState(false);
  const [pending, setPending] = useState(null); // { lat, lng }
  const [hostelName, setHostelName] = useState('');
  const [hostelArea, setHostelArea] = useState('ward-ktr-04');
  const [saving, setSaving] = useState(false);
  const [saveMsg, setSaveMsg] = useState(null);
  const addModeRef = useRef(false);
  addModeRef.current = addMode;
  const [selectedMapItem, setSelectedMapItem] = useState(null);
  const [mapLoaded, setMapLoaded] = useState(false);
  
  const mapContainerRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const markersLayerRef = useRef(null);

  // Initialize Leaflet Map
  useEffect(() => {
    if (!mapContainerRef.current) return;
    
    // Check if Leaflet window.L is available
    if (typeof window.L === 'undefined') {
      console.warn('Leaflet library loading...');
      return;
    }

    const L = window.L;

    // Destroy existing map instance if any
    if (mapInstanceRef.current) {
      mapInstanceRef.current.remove();
      mapInstanceRef.current = null;
    }

    // Create Map Instance centered at SRM KTR
    const map = L.map(mapContainerRef.current, {
      center: [CAMPUS_DEFAULT_LAT, CAMPUS_DEFAULT_LNG],
      zoom: CAMPUS_DEFAULT_ZOOM,
      zoomControl: true,
      attributionControl: true
    });

    // Add Pure Open-Source OpenStreetMap Tile Layer (No API Key Required)
    L.tileLayer(OSM_TILE_URL || 'https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a> contributors',
      maxZoom: 19
    }).addTo(map);

    // Create a LayerGroup for dynamic markers
    const markersGroup = L.layerGroup().addTo(map);
    markersLayerRef.current = markersGroup;
    mapInstanceRef.current = map;
    map.on('click', (e) => {
      if (addModeRef.current) setPending({ lat: Number(e.latlng.lat.toFixed(6)), lng: Number(e.latlng.lng.toFixed(6)) });
    });
    setMapLoaded(true);

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
    };
  }, []);

  // Update Markers when data or filter changes
  useEffect(() => {
    const map = mapInstanceRef.current;
    const markersGroup = markersLayerRef.current;
    const L = window.L;

    if (!map || !markersGroup || !L) return;

    markersGroup.clearLayers();

    // 1. SMART BINS LAYER
    if (filterType === 'all' || filterType === 'bins') {
      bins.forEach(bin => {
        if (!bin.coordinates || !bin.coordinates.lat || !bin.coordinates.lng) return;

        const color = '#10B981';

        const customIcon = L.divIcon({
          className: 'custom-bin-marker',
          html: `
            <div style="
              width: 32px;
              height: 32px;
              border-radius: 50%;
              background: ${color};
              border: 2px solid #FFFFFF;
              display: flex;
              align-items: center;
              justify-content: center;
              box-shadow: 0 4px 10px rgba(0,0,0,0.4);
              cursor: pointer;
            ">
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="#FFFFFF" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14M10 10v6M14 10v6"/></svg>
            </div>
          `,
          iconSize: [32, 32],
          iconAnchor: [16, 16]
        });

        const marker = L.marker([bin.coordinates.lat, bin.coordinates.lng], { icon: customIcon });
        
        marker.on('click', () => {
          setSelectedMapItem({ ...bin, itemType: 'bin' });
        });

        marker.bindTooltip(`<b>${bin.code}</b><br/>${bin.location}`, {
          direction: 'top',
          offset: [0, -12]
        });

        markersGroup.addLayer(marker);
      });
    }

    // 2. COMPLAINTS LAYER
    if (false) { // map shows bins only
      complaints.forEach(cmp => {
        if (!cmp.coordinates || !cmp.coordinates.lat || !cmp.coordinates.lng) return;

        const color = cmp.priority === 'Critical' ? '#EF4444' : cmp.priority === 'High' ? '#F97316' : '#3B82F6';

        const customIcon = L.divIcon({
          className: 'custom-cmp-marker',
          html: `
            <div style="
              width: 30px;
              height: 30px;
              border-radius: 8px;
              background: ${color};
              border: 2px solid #FFFFFF;
              display: flex;
              align-items: center;
              justify-content: center;
              box-shadow: 0 4px 12px rgba(0,0,0,0.5);
              cursor: pointer;
            ">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#FFFFFF" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/>
                <line x1="12" y1="9" x2="12" y2="13"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
            </div>
          `,
          iconSize: [30, 30],
          iconAnchor: [15, 15]
        });

        const marker = L.marker([cmp.coordinates.lat, cmp.coordinates.lng], { icon: customIcon });

        marker.on('click', () => {
          setSelectedMapItem({ ...cmp, itemType: 'complaint' });
        });

        marker.bindTooltip(`<b>${cmp.ticketNo}</b><br/>${cmp.category} - ${cmp.location}`, {
          direction: 'top',
          offset: [0, -12]
        });

        markersGroup.addLayer(marker);
      });
    }


    // 2c. HOSTEL BLOCKS LAYER (Firestore `households`: green + tick once collected today)
    if (filterType === 'all' || filterType === 'hostels') {
      const today = toDateString();
      households.forEach((h) => {
        const lat = Number(h.lat);
        const lng = Number(h.lng);
        if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;
        const done = h.collectedDate === today;
        // Men's (boys) hostels are blue, women's (girls) hostels are pink; a green ring + tick means collected today.
        const zone = (zones.length ? zones : DEFAULT_ZONES).find((z) => z.id === h.areaId);
        const girls = zone?.group === 'girls' || h.areaId === 'ward-ktr-06';
        const color = girls ? '#EC4899' : '#2563EB';
        const ring = done ? '#22C55E' : '#FFFFFF';
        const glyph = done ? '<path d="M20 6 9 17l-5-5"/>' : '<path d="M3 11l9-8 9 8M5 10v10h14V10"/>';
        const icon = L.divIcon({
          className: 'custom-hostel-marker',
          html: `<div style="width:32px;height:32px;border-radius:10px;background:${color};border:${done ? 4 : 2}px solid ${ring};display:flex;align-items:center;justify-content:center;box-shadow:0 4px 10px rgba(0,0,0,0.4);cursor:pointer;"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#FFFFFF" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">${glyph}</svg></div>`,
          iconSize: [32, 32],
          iconAnchor: [16, 16]
        });
        const marker = L.marker([lat, lng], { icon, zIndexOffset: 300 });
        marker.on('click', () => setSelectedMapItem({ ...h, itemType: 'hostel', coordinates: { lat, lng } }));
        marker.bindTooltip(`<b>${h.name || h.code}</b><br/>${girls ? "Women's (Girls)" : "Men's (Boys)"} · ${done ? 'Collected today' : 'Waiting for collection'}`, {
          direction: 'top',
          offset: [0, -12]
        });
        markersGroup.addLayer(marker);
      });
    }

    // Pin being placed in add-hostel mode
    if (pending) {
      markersGroup.addLayer(L.circleMarker([pending.lat, pending.lng], {
        radius: 11, color: '#FFFFFF', weight: 3, fillColor: '#38BDF8', fillOpacity: 0.95
      }));
    }

    // 3. VEHICLES / WORKERS LAYER
    if (false) { // map shows bins only
      workers.forEach((worker, idx) => {
        const lat = 12.8235 + (idx * 0.0012) * (idx % 2 === 0 ? 1 : -1);
        const lng = 80.0442 + (idx * 0.0015) * (idx % 2 === 0 ? -1 : 1);

        const customIcon = L.divIcon({
          className: 'custom-truck-marker',
          html: `
            <div style="
              width: 32px;
              height: 32px;
              border-radius: 50%;
              background: #0284C7;
              border: 2px solid #38BDF8;
              display: flex;
              align-items: center;
              justify-content: center;
              box-shadow: 0 4px 10px rgba(0,0,0,0.5);
              cursor: pointer;
            ">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#FFFFFF" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <rect width="16" height="13" x="1" y="6" rx="2"/>
                <path d="M16 8h4l3 3v5h-7V8z"/>
                <circle cx="5.5" cy="18.5" r="2.5"/>
                <circle cx="18.5" cy="18.5" r="2.5"/>
              </svg>
            </div>
          `,
          iconSize: [32, 32],
          iconAnchor: [16, 16]
        });

        const marker = L.marker([lat, lng], { icon: customIcon });

        marker.on('click', () => {
          setSelectedMapItem({ ...worker, itemType: 'worker', coordinates: { lat, lng } });
        });

        marker.bindTooltip(`<b>${worker.name}</b><br/>${worker.role || 'Collection Vehicle'}`, {
          direction: 'top',
          offset: [0, -12]
        });

        markersGroup.addLayer(marker);
      });
    }

  }, [bins, complaints, workers, schedules, households, pending, filterType, mapLoaded]);

  // Crosshair while placing a hostel block
  useEffect(() => {
    if (mapContainerRef.current) mapContainerRef.current.style.cursor = addMode ? 'crosshair' : '';
  }, [addMode]);

  const handleSaveHostel = async (e) => {
    e.preventDefault();
    if (!pending || !hostelName.trim() || !onAddHostel) return;
    setSaving(true);
    setSaveMsg(null);
    try {
      const area = (zones.length ? zones : DEFAULT_ZONES).find((w) => w.id === hostelArea);
      const code = await onAddHostel({ name: hostelName.trim(), areaId: hostelArea, areaName: area?.name || '', lat: pending.lat, lng: pending.lng });
      setSaveMsg({ ok: true, text: `${hostelName.trim()} saved as ${code}.` });
      setHostelName('');
      setPending(null);
    } catch (err) {
      setSaveMsg({ ok: false, text: /permission|insufficient/i.test(err.message || '') ? 'Firebase refused the write: deploy the updated Firestore rules.' : `Could not save: ${err.message}` });
    } finally {
      setSaving(false);
    }
  };

  // Jump to specific POI on SRM KTR
  const handleJumpToPoi = (poi) => {
    if (mapInstanceRef.current) {
      mapInstanceRef.current.flyTo([poi.lat, poi.lng], poi.zoom, {
        duration: 1.2
      });
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Map Control Bar */}
      <div className="glass-panel" style={{ padding: '16px 22px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <h2 style={{ fontSize: '19px', fontWeight: 800 }}>Campus Map</h2>
            <span style={{ 
              fontSize: '11px', 
              fontWeight: 800, 
              background: 'rgba(16, 185, 129, 0.15)', 
              color: 'var(--accent-emerald)', 
              padding: '3px 8px', 
              borderRadius: '8px',
              border: '1px solid rgba(16, 185, 129, 0.3)'
            }}>
              📍 {CAMPUS_NAME}
            </span>
          </div>
          <p style={{ fontSize: '12.5px', color: 'var(--text-secondary)' }}>
            Bin locations and hostel blocks on the SRM KTR campus.
          </p>
        </div>

        {/* Layer Filters */}
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          <button onClick={() => setFilterType('all')} className={`btn btn-sm ${filterType === 'all' ? 'btn-primary' : 'btn-secondary'}`}>
            <Layers size={14} />
            <span>All</span>
          </button>
          <button 
            onClick={() => setFilterType('bins')} 
            className={`btn btn-sm ${filterType === 'bins' ? 'btn-primary' : 'btn-secondary'}`}
          >
            <Trash2 size={14} />
            <span>Bin Locations ({bins.length})</span>
          </button>
          <button onClick={() => setFilterType('hostels')} className={`btn btn-sm ${filterType === 'hostels' ? 'btn-primary' : 'btn-secondary'}`}>
            <Home size={14} />
            <span>Hostel Blocks ({households.length})</span>
          </button>
          {onAddHostel && (
            <button
              onClick={() => { setAddMode((v) => !v); setPending(null); setSaveMsg(null); if (!addMode && filterType === 'bins') setFilterType('all'); }}
              className={`btn btn-sm ${addMode ? 'btn-primary' : 'btn-secondary'}`}
            >
              <Plus size={14} />
              <span>{addMode ? 'Click the map to place…' : 'Add hostel block'}</span>
            </button>
          )}
        </div>
      </div>

      {/* Campus Quick Jump Selector Bar */}
      <div className="glass-panel" style={{ padding: '10px 18px', display: 'flex', alignItems: 'center', gap: '8px', overflowX: 'auto' }}>
        <span style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
          Quick Jump:
        </span>
        {CAMPUS_POIS.map((poi, idx) => (
          <button
            key={idx}
            onClick={() => handleJumpToPoi(poi)}
            className="btn btn-sm btn-secondary"
            style={{ fontSize: '11.5px', padding: '4px 10px', borderRadius: '12px', whiteSpace: 'nowrap' }}
          >
            <Compass size={12} style={{ color: 'var(--accent-cyan)' }} />
            <span>{poi.name}</span>
          </button>
        ))}
      </div>

      {/* Main Map Canvas Area */}
      <div style={{ display: 'grid', gridTemplateColumns: (selectedMapItem || pending) ? '1fr 340px' : '1fr', gap: '20px' }}>
        <div 
          className="glass-panel" 
          style={{ 
            height: '620px', 
            position: 'relative', 
            overflow: 'hidden',
            borderRadius: '20px',
            border: '1px solid var(--glass-border)'
          }}
        >
          <div 
            ref={mapContainerRef} 
            style={{ width: '100%', height: '100%', zIndex: 1 }} 
          />

          {/* Hostel colour legend */}
          <div style={{
            position: 'absolute', top: '16px', right: '16px', zIndex: 1000,
            background: 'rgba(15, 23, 42, 0.9)', backdropFilter: 'blur(10px)',
            border: '1px solid var(--glass-border)', borderRadius: '12px', padding: '10px 14px',
            display: 'grid', gap: '6px', fontSize: '11.5px', fontWeight: 700, color: 'var(--text-primary)'
          }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}><i style={{ width: 14, height: 14, borderRadius: 4, background: '#2563EB', border: '2px solid #FFF', display: 'inline-block' }} /> Men's (Boys) hostel</span>
            <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}><i style={{ width: 14, height: 14, borderRadius: 4, background: '#EC4899', border: '2px solid #FFF', display: 'inline-block' }} /> Women's (Girls) hostel</span>
            <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}><i style={{ width: 14, height: 14, borderRadius: 4, background: '#64748B', border: '3px solid #22C55E', display: 'inline-block' }} /> Collected today</span>
            <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}><i style={{ width: 14, height: 14, borderRadius: 7, background: '#10B981', border: '2px solid #FFF', display: 'inline-block' }} /> Bin</span>
          </div>

          {/* Floating Campus Badge */}
          <div style={{
            position: 'absolute',
            bottom: '16px',
            left: '16px',
            zIndex: 1000,
            background: 'rgba(15, 23, 42, 0.88)',
            backdropFilter: 'blur(10px)',
            border: '1px solid var(--glass-border)',
            borderRadius: '12px',
            padding: '8px 14px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            fontSize: '11.5px',
            fontWeight: 700,
            color: 'var(--text-primary)'
          }}>
            <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#10B981', boxShadow: '0 0 8px #10B981' }} />
            <span>OpenStreetMap Engine • SRM KTR Campus Grid</span>
          </div>
        </div>

        {/* New hostel block form (add mode) */}
        {pending && (
          <form onSubmit={handleSaveHostel} className="glass-panel" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '14px', height: '620px', overflowY: 'auto' }}>
            <span style={{ fontSize: '12px', fontWeight: 800, color: 'var(--accent-cyan)', textTransform: 'uppercase' }}>New hostel block</span>
            <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
              Location: <b>{pending.lat}, {pending.lng}</b><br/>Click elsewhere on the map to move the pin.
            </div>
            <input className="input-field" placeholder="Hostel block name" value={hostelName} onChange={(e) => setHostelName(e.target.value)} autoFocus />
            <select className="input-field" value={hostelArea} onChange={(e) => setHostelArea(e.target.value)}>
              {(zones.length ? zones : DEFAULT_ZONES).map((w) => (
                <option key={w.id} value={w.id} style={{ background: '#0D1322' }}>{w.name}</option>
              ))}
            </select>
            <button type="submit" className="btn btn-primary" disabled={saving || !hostelName.trim()}>{saving ? 'Saving…' : 'Save to Firestore'}</button>
            <button type="button" className="btn btn-secondary" onClick={() => setPending(null)}>Cancel</button>
            {saveMsg && <div style={{ fontSize: '12px', color: saveMsg.ok ? '#6EE7B7' : '#FCA5A5' }}>{saveMsg.text}</div>}
          </form>
        )}

        {/* Selected Item Detail Sidebar */}
        {selectedMapItem && !pending && (
          <div className="glass-panel" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '16px', height: '620px', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '12px', fontWeight: 800, color: 'var(--accent-cyan)', textTransform: 'uppercase' }}>
                {selectedMapItem.itemType === 'bin' ? 'Bin Location' : selectedMapItem.itemType === 'hostel' ? 'Hostel Block' : selectedMapItem.itemType === 'complaint' ? 'Citizen Grievance' : 'Fleet Unit'}
              </span>
              <button 
                onClick={() => setSelectedMapItem(null)} 
                className="btn btn-sm btn-secondary" 
                style={{ padding: '4px', borderRadius: '50%' }}
              >
                <X size={14} />
              </button>
            </div>

            {selectedMapItem.itemType === 'bin' && (
              <>
                <div>
                  <h3 style={{ fontSize: '18px', fontWeight: 800 }}>{selectedMapItem.code}</h3>
                  <p style={{ fontSize: '12.5px', color: 'var(--text-secondary)' }}>📍 {selectedMapItem.location}</p>
                </div>

                <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                  <b>Waste Category:</b> {selectedMapItem.type}
                </div>

                <a 
                  href={`https://www.openstreetmap.org/?mlat=${selectedMapItem.coordinates.lat}&mlon=${selectedMapItem.coordinates.lng}#map=18/${selectedMapItem.coordinates.lat}/${selectedMapItem.coordinates.lng}`}
                  target="_blank"
                  rel="noreferrer"
                  className="btn btn-sm btn-secondary"
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}
                >
                  <ExternalLink size={14} />
                  <span>View on OpenStreetMap</span>
                </a>
              </>
            )}

            {selectedMapItem.itemType === 'hostel' && (
              <>
                <div>
                  <h3 style={{ fontSize: '18px', fontWeight: 800, color: selectedMapItem.areaId === 'ward-ktr-06' ? '#F472B6' : '#60A5FA' }}>{selectedMapItem.name || selectedMapItem.code}</h3>
                  <p style={{ fontSize: '12.5px', color: 'var(--text-secondary)' }}>{selectedMapItem.code} · {selectedMapItem.areaName}</p>
                </div>
                <div style={{ fontSize: '12.5px', fontWeight: 700, color: selectedMapItem.collectedDate === toDateString() ? '#34D399' : '#FBBF24' }}>
                  {selectedMapItem.collectedDate === toDateString()
                    ? `Collected today${selectedMapItem.collectedByName ? ' by ' + selectedMapItem.collectedByName : ''}`
                    : 'Waiting for collection'}
                </div>
                {selectedMapItem.ownerName && (
                  <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>Linked resident: <b>{selectedMapItem.ownerName}</b></div>
                )}
              </>
            )}

            {selectedMapItem.itemType === 'complaint' && (
              <>
                <div>
                  <h3 style={{ fontSize: '18px', fontWeight: 800 }}>{selectedMapItem.ticketNo}</h3>
                  <p style={{ fontSize: '12.5px', color: 'var(--text-secondary)' }}>📍 {selectedMapItem.location}</p>
                </div>

                <div style={{ display: 'flex', gap: '8px' }}>
                  <StatusBadge status={selectedMapItem.status} />
                  <span style={{ fontSize: '11px', padding: '3px 8px', borderRadius: '6px', background: 'rgba(255,255,255,0.06)', fontWeight: 700 }}>
                    Priority: {selectedMapItem.priority}
                  </span>
                </div>

                <div style={{ padding: '12px', borderRadius: '12px', background: 'rgba(255,255,255,0.03)' }}>
                  <div style={{ fontSize: '11.5px', color: 'var(--text-muted)', marginBottom: '4px' }}>Issue Description</div>
                  <div style={{ fontSize: '12.5px' }}>{selectedMapItem.description}</div>
                </div>

                <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                  <b>Reported By:</b> {selectedMapItem.citizen?.name} ({selectedMapItem.citizen?.badge})<br/>
                  <b>Reported Time:</b> {selectedMapItem.reportedAt}
                </div>

                <button 
                  onClick={() => onSelectComplaint && onSelectComplaint(selectedMapItem)}
                  className="btn btn-primary btn-sm"
                  style={{ marginTop: 'auto' }}
                >
                  Open in Complaints Console
                </button>
              </>
            )}

            {selectedMapItem.itemType === 'worker' && (
              <>
                <div>
                  <h3 style={{ fontSize: '18px', fontWeight: 800 }}>{selectedMapItem.name}</h3>
                  <p style={{ fontSize: '12.5px', color: 'var(--text-secondary)' }}>🆔 {selectedMapItem.id || 'WRK-KTR'}</p>
                </div>

                <div style={{ padding: '12px', borderRadius: '12px', background: 'rgba(255,255,255,0.03)' }}>
                  <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Assigned Zone</div>
                  <div style={{ fontSize: '13px', fontWeight: 700 }}>{selectedMapItem.wardAssigned || 'SRM KTR Campus'}</div>
                </div>

                <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                  <b>Vehicle Type:</b> Electric Tipper (Campus Clean)<br/>
                  <b>Shift Status:</b> On Active Duty
                </div>
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
