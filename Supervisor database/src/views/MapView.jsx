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
  ExternalLink
} from 'lucide-react';
import StatusBadge from '../components/StatusBadge';

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
  onSelectComplaint 
}) {
  const [filterType, setFilterType] = useState('all'); // all | bins | complaints | trucks
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

  }, [bins, complaints, workers, filterType, mapLoaded]);

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
            Bin locations on the SRM KTR campus.
          </p>
        </div>

        {/* Layer Filters */}
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          <button 
            onClick={() => setFilterType('bins')} 
            className={`btn btn-sm ${filterType === 'bins' ? 'btn-primary' : 'btn-secondary'}`}
          >
            <Trash2 size={14} />
            <span>Bin Locations ({bins.length})</span>
          </button>
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
      <div style={{ display: 'grid', gridTemplateColumns: selectedMapItem ? '1fr 340px' : '1fr', gap: '20px' }}>
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

        {/* Selected Item Detail Sidebar */}
        {selectedMapItem && (
          <div className="glass-panel" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '16px', height: '620px', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '12px', fontWeight: 800, color: 'var(--accent-cyan)', textTransform: 'uppercase' }}>
                {selectedMapItem.itemType === 'bin' ? 'Bin Location' : selectedMapItem.itemType === 'complaint' ? 'Citizen Grievance' : 'Fleet Unit'}
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
