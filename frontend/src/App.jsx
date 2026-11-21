import React, { useCallback, useEffect, useState } from 'react';

import { AlertCircle, Bell, Check, CheckCircle2, ChevronDown, ClipboardList, Droplet, HeartPulse, Home, Hospital, LogOut, Menu, Plus, RefreshCw, Search, ShieldCheck, Users, Warehouse, X, UserRound, MapPin, Package, Eye, EyeOff } from 'lucide-react';

import { api, normalizeList } from './api';

import './App.css';



const GROUPS = ['A+','A-','B+','B-','AB+','AB-','O+','O-'];

const ROLES = ['USER','DONOR','HOSPITAL','BLOOD_BANK'];

const fmt = (v) => v ? new Date(v).toLocaleString() : '—';

const val = (o, ...keys) => { for (const k of keys) if (o?.[k] !== undefined && o?.[k] !== null && o?.[k] !== '') return o[k]; return '—'; };



const NAV_BY_ROLE = {
    USER: [
      ['dashboard','Dashboard',Home], ['requests','Blood Requests',ClipboardList],
      ['donors','Find Donors',Users], ['notifications','Notifications',Bell], ['profile','My Profile',UserRound]
    ],

    DONOR: [

      ['dashboard','Dashboard',Home], ['requests','Blood Requests',ClipboardList],

      ['donors','My Donor Profile',Users], ['notifications','Notifications',Bell], ['profile','My Profile',UserRound]

    ],

    HOSPITAL: [

      ['dashboard','Dashboard',Home], ['requests','Blood Requests',ClipboardList],

      ['donors','Find Donors',Users], ['hospitals','Hospital Profile',Hospital], ['banks','Blood Banks',Warehouse],

      ['notifications','Notifications',Bell], ['profile','My Profile',UserRound]

    ],

    BLOOD_BANK: [

      ['dashboard','Dashboard',Home], ['requests','Blood Requests',ClipboardList],

      ['donors','Find Donors',Users], ['hospitals','Hospitals',Hospital], ['banks','Blood Bank Profile',Warehouse],

      ['inventory','Blood Inventory',Package], ['notifications','Notifications',Bell],

      ['profile','My Profile',UserRound]

    ],

    ADMIN: [

      ['dashboard','Dashboard',Home], ['requests','Blood Requests',ClipboardList],

      ['donors','Donors',Users], ['hospitals','Hospitals',Hospital],

      ['banks','Blood Banks',Warehouse], ['inventory','Blood Inventory',Package],

      ['notifications','Notifications',Bell], ['profile','My Profile',UserRound],

      ['admin','User Management',ShieldCheck]

    ]

  };





function App() {

  const [token, setToken] = useState(localStorage.getItem(api.tokenKey));

  const [user, setUser] = useState(null);

  const [page, setPage] = useState('dashboard');

  const [collapsed, setCollapsed] = useState(false);

  const [notice, setNotice] = useState(null);

  const [unread, setUnread] = useState(0);

  const [refresh, setRefresh] = useState(0);

  const toast = useCallback((message, type='ok') => { setNotice({message,type}); window.clearTimeout(window.__bbToast); window.__bbToast = window.setTimeout(()=>setNotice(null), 3800); }, []);

  const logout = () => { localStorage.removeItem(api.tokenKey); setToken(null); setUser(null); setPage('dashboard'); };

  useEffect(() => { if (!token) return; api.me().then(setUser).catch(e => { toast(e.message,'error'); localStorage.removeItem(api.tokenKey); setToken(null); setUser(null); }); }, [token, toast]);

  useEffect(() => { if (!token) return; let alive=true; const load=()=>api.unreadCount().then(x=>{if(alive)setUnread(Number(x?.count||0))}).catch(()=>{}); load(); const timer=window.setInterval(load,20000); return ()=>{alive=false;window.clearInterval(timer)}; }, [token, refresh]);

  const role = String(user?.role || '').toUpperCase();

  const isAdmin = role === 'ADMIN';

  // Navigation is intentionally role-specific. Hiding a menu item is a UI

  // convenience only; backend authorization must still protect every API.

  const nav = React.useMemo(() => NAV_BY_ROLE[role] || NAV_BY_ROLE.DONOR, [role]);

  const allowedPages = React.useMemo(() => nav.map(([key]) => key), [nav]);

    useEffect(() => {

    if (user && !allowedPages.includes(page)) setPage('dashboard');

  }, [role, page, user, allowedPages]);

  if (!token) return <Auth onLogin={(t,u)=>{localStorage.setItem(api.tokenKey,t);setToken(t);setUser(u);}} toast={toast}/>;

  const reload = () => setRefresh(v=>v+1);

  return <div className="app-shell">

    <aside className={`sidebar ${collapsed?'collapsed':''}`}>

      <div className="brand"><span className="brand-mark"><Droplet size={20}/></span><span>Blood<span className="brand-light">Bridge</span></span><button className="collapse" onClick={()=>setCollapsed(!collapsed)}><Menu size={18}/></button></div>

      <div className="side-caption">WORKSPACE</div>

      <nav>{nav.map(([key,label,Icon])=><button key={key} className={`nav-item ${page===key?'active':''}`} onClick={()=>setPage(key)}><Icon size={17}/><span>{label}</span>{key==='notifications'&&unread>0&&<b className="nav-count">{unread}</b>}</button>)}</nav>

      <div className="side-bottom"><div className="side-help"><HeartPulse size={19}/><div><b>Every drop counts</b><small>Connecting lives, saving lives.</small></div></div><button className="nav-item logout" onClick={logout}><LogOut size={17}/><span>Sign out</span></button></div>

    </aside>

    <main className="main-area"><header className="topbar"><div><div className="eyebrow">BLOOD DONOR EMERGENCY SYSTEM</div><h2>{nav.find(n=>n[0]===page)?.[1] || 'Dashboard'}</h2></div><div className="top-actions"><button className="icon-btn notification-btn" onClick={()=>setPage('notifications')}><Bell size={19}/>{unread>0&&<i/>}</button><div className="user-chip"><div className="avatar">{(user?.name||'U').slice(0,1).toUpperCase()}</div><div><b>{user?.name||'User'}</b><small>{role.replace('_',' ')}</small></div><ChevronDown size={15}/></div></div></header>

      <section className="page-content" key={page}>{page==='dashboard'&&<Dashboard user={user} role={role} go={setPage} reload={refresh} toast={toast}/>}{page==='requests'&&allowedPages.includes('requests')&&<Requests role={role} user={user} toast={toast} reload={reload}/>}{page==='donors'&&allowedPages.includes('donors')&&<Donors role={role} toast={toast} reload={reload}/>}{page==='hospitals'&&allowedPages.includes('hospitals')&&<Hospitals role={role} toast={toast}/>}{page==='banks'&&allowedPages.includes('banks')&&<Banks role={role} toast={toast}/>}{page==='inventory'&&allowedPages.includes('inventory')&&<Inventory toast={toast} role={role}/>}{page==='notifications'&&allowedPages.includes('notifications')&&<Notifications toast={toast} onChange={reload}/>}{page==='profile'&&allowedPages.includes('profile')&&<Profile user={user} setUser={setUser} toast={toast} logout={logout}/>}{page==='admin'&&isAdmin&&<Admin toast={toast}/>}</section>

    </main>

    {notice&&<div className={`toast ${notice.type}`}><span>{notice.type==='error'?<AlertCircle size={17}/>:<CheckCircle2 size={17}/>}</span>{notice.message}<button onClick={()=>setNotice(null)}><X size={15}/></button></div>}

  </div>;

}



function Auth({ onLogin, toast }) {
  const [mode, setMode] = useState('login');
  const [busy, setBusy] = useState(false);
  const [show, setShow] = useState(false);
  const [otpSent, setOtpSent] = useState(false);
  const [signupOtpSent, setSignupOtpSent] = useState(false);
  const [signupOtpSeconds, setSignupOtpSeconds] = useState(0);
  const [form, setForm] = useState({
    email: '', password: '', name: '', phone: '', gender: '',
    profession: '', location: '', role: 'USER', otp: '', newPassword: '',
    emailOtp: ''
  });

  useEffect(() => {
    if (!signupOtpSent || signupOtpSeconds <= 0) return undefined;
    const timer = window.setTimeout(() => {
      setSignupOtpSeconds(seconds => Math.max(0, seconds - 1));
    }, 1000);
    return () => window.clearTimeout(timer);
  }, [signupOtpSent, signupOtpSeconds]);

  const set = (key, value) => {
    const isSignupDetail = ['email', 'password', 'name', 'phone', 'gender', 'profession', 'location', 'role'].includes(key);
    if (mode === 'signup' && signupOtpSent && isSignupDetail) {
      setSignupOtpSent(false);
      setSignupOtpSeconds(0);
      setForm(prev => ({ ...prev, [key]: value, emailOtp: '' }));
      return;
    }
    setForm(prev => ({ ...prev, [key]: value }));
  };

  const resetForgotForm = () => {
    setOtpSent(false);
    setForm(prev => ({ ...prev, otp: '', newPassword: '' }));
  };

  const signupPayload = () => ({
    name: form.name.trim(),
    email: form.email.trim(),
    phone: form.phone.trim(),
    password: form.password,
    gender: form.gender.trim(),
    profession: form.profession.trim(),
    location: form.location.trim(),
    role: form.role
  });

  const requestSignupOtps = async () => {
    const payload = signupPayload();
    if (payload.password.length < 6) throw new Error('Password must be at least 6 characters.');
    const response = await api.sendSignupOtp(payload);
    setSignupOtpSent(true);
    setSignupOtpSeconds(Number(response?.expiresInSeconds) || 60);
    setForm(prev => ({ ...prev, emailOtp: '' }));
    toast(response?.message || 'Email OTP sent. Check your inbox and spam folder.');
  };

  const requestForgotOtp = async () => {
    const email = form.email.trim();
    if (!email) {
      toast('Please enter your email address.', 'error');
      return;
    }
    setBusy(true);
    try {
      const response = await api.forgot(email);
      setOtpSent(true);
      toast(response?.devOtp
        ? `Development OTP: ${response.devOtp}`
        : response?.message || 'OTP sent. Please check your email.');
    } catch (error) {
      toast(error.message || 'Unable to send OTP.', 'error');
    } finally {
      setBusy(false);
    }
  };

  const submit = async (e) => {
    e.preventDefault();
    if (busy) return;

    if (mode === 'forgot' && !otpSent) {
      await requestForgotOtp();
      return;
    }

    setBusy(true);
    try {
      if (mode === 'signup' && !signupOtpSent) {
        await requestSignupOtps();
      } else if (mode === 'signup') {
        if (!/^\d{6}$/.test(form.emailOtp)) {
          throw new Error('Enter the 6-digit OTP sent to your email.');
        }
        const response = await api.verifySignupOtp({
          email: form.email.trim(),
          emailOtp: form.emailOtp
        });
        toast(response?.message || 'Email verified. Account created. Please sign in.');
        setMode('login');
        setSignupOtpSent(false);
        setSignupOtpSeconds(0);
        setForm(prev => ({ ...prev, password: '', emailOtp: '' }));
      } else if (mode === 'forgot') {
        if (!/^\d{6}$/.test(form.otp)) {
          throw new Error('Please enter a valid 6-digit OTP.');
        }
        if (form.newPassword.length < 8) {
          throw new Error('New password must contain at least 8 characters.');
        }
        const response = await api.reset({
          email: form.email.trim(),
          otp: form.otp,
          newPassword: form.newPassword
        });
        toast(response?.message || 'Password reset successfully. Please sign in.');
        setForm(prev => ({ ...prev, password: '', otp: '', newPassword: '' }));
        setMode('login');
        setOtpSent(false);
      } else {
        const response = await api.login({
          email: form.email.trim(),
          password: form.password
        });
        const account = response?.user || response?.data?.user || response?.data || response;
        const token = response?.token || response?.accessToken || response?.data?.token;
        if (!token) throw new Error('Login response did not contain a token.');
        onLogin(token, {
          id: account?.userId ?? account?.id ?? response?.userId,
          name: account?.name ?? response?.name,
          email: account?.email ?? response?.email,
          role: account?.role ?? response?.role
        });
      }
    } catch (error) {
      toast(error.message || 'Something went wrong.', 'error');
    } finally {
      setBusy(false);
    }
  };

  const switchMode = (nextMode) => {
    setMode(nextMode);
    setOtpSent(false);
    setSignupOtpSent(false);
    setSignupOtpSeconds(0);
    setForm(prev => ({ ...prev, otp: '', newPassword: '', emailOtp: '' }));
  };

  return (
    <div className="auth-wrap">
      <div className="auth-art">
        <div className="auth-brand"><Droplet /> BloodBridge</div>
        <div className="auth-message">
          <span className="pill">DONATE BLOOD · SAVE LIVES</span>
          <h1>A small act.<br /><em>A lifetime of hope.</em></h1>
          <p>Join a connected network of donors, hospitals and blood banks. Help the right blood reach the right person.</p>
          <div className="auth-stat">
            <div><b>8</b><small>Blood groups</small></div>
            <div><b>24/7</b><small>Emergency support</small></div>
            <div><b>1</b><small>Mission · Save lives</small></div>
          </div>
        </div>
        <div className="auth-foot">A community-powered emergency blood network</div>
      </div>

      <div className="auth-panel">
        <form className="auth-form" onSubmit={submit} autoComplete="off">
          <div className="mobile-brand"><Droplet /> BloodBridge</div>
          <span className="eyebrow">WELCOME TO BLOODBRIDGE</span>
          <h2>{mode === 'signup' ? 'Create your account' : mode === 'forgot' ? 'Reset your password' : 'Sign in to continue'}</h2>
          <p className="muted">
            {mode === 'signup'
              ? 'Verify your email address to register. Your phone number is kept as contact information.'
              : 'Your account connects you to the people who need you.'}
          </p>

          {mode === 'signup' && (
            <>
              <Field label="Full name" value={form.name} onChange={v => set('name', v)} required />
              <Field label="Phone number" value={form.phone} onChange={v => set('phone', v)} placeholder="10-digit mobile number" required />
              <div className="form-grid">
                <Field label="Gender" value={form.gender} onChange={v => set('gender', v)} placeholder="Male / Female / Other" required />
                <Field label="City" value={form.location} onChange={v => set('location', v)} required />
              </div>
              <Field label="Profession" value={form.profession} onChange={v => set('profession', v)} />
              <Select label="Account type" value={form.role} onChange={v => set('role', v)} options={ROLES.map(x => ({ value: x, label: x.replace('_', ' ') }))} />
            </>
          )}

          <Field
            label="Email address"
            type="email"
            value={form.email}
            onChange={v => {
              set('email', v);
              if (mode === 'forgot') resetForgotForm();
            }}
            required
            autoComplete="email"
          />

          {mode !== 'forgot' && (
            <div className="password-wrap">
              <Field
                label={mode === 'signup' ? 'Create password' : 'Password'}
                type={show ? 'text' : 'password'}
                value={form.password}
                onChange={v => set('password', v)}
                placeholder={mode === 'signup' ? 'Minimum 6 characters' : 'Enter your password'}
                required
                autoComplete={mode === 'signup' ? 'new-password' : 'current-password'}
              />
              <button type="button" className="show-pass" onClick={() => setShow(!show)} aria-label={show ? 'Hide password' : 'Show password'}>
                {show ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          )}

          {mode === 'signup' && signupOtpSent && (
            <div className="form-card" style={{ padding: 14, marginTop: 4 }}>
              <b>Verify your email address</b>
              <p className="muted" style={{ margin: '6px 0 12px' }}>Enter the 6-digit code sent to your email. OTP expires in 60 seconds. Your phone number is saved for contact; no SMS verification is required.</p>
              <Field label="Email OTP" value={form.emailOtp} onChange={v => set('emailOtp', v.replace(/\D/g, '').slice(0, 6))} placeholder="6-digit code" required />
              <button type="button" className="text-link" disabled={busy || signupOtpSeconds > 0} onClick={async () => {
                setBusy(true);
                try { await requestSignupOtps(); }
                catch (error) { toast(error.message || 'Unable to resend email OTP.', 'error'); }
                finally { setBusy(false); }
              }}>{signupOtpSeconds > 0 ? `Resend Email OTP in ${signupOtpSeconds}s` : 'Resend Email OTP'}</button>
            </div>
          )}

          {mode === 'forgot' && (
            otpSent ? (
              <>
                <Field label="OTP (6 digits)" value={form.otp} onChange={v => set('otp', v.replace(/\D/g, '').slice(0, 6))} placeholder="Enter OTP" required />
                <Field label="New password" type="password" value={form.newPassword} onChange={v => set('newPassword', v)} placeholder="Minimum 8 characters" required />
              </>
            ) : (
              <p className="muted">Enter your registered email address. We will send you an OTP.</p>
            )
          )}

          <button className="btn-primary full" disabled={busy} type="submit">
            {busy && <RefreshCw className="spin" size={16} />}
            {mode === 'signup'
              ? (signupOtpSent ? 'Verify Email OTP & Create Account' : 'Send Email OTP')
              : mode === 'forgot' ? (otpSent ? 'Verify OTP & Reset Password' : 'Send OTP')
              : 'Sign in'}
          </button>

          {mode === 'forgot' && otpSent && (
            <button type="button" className="text-link" disabled={busy} onClick={requestForgotOtp}>Resend OTP</button>
          )}

          {mode === 'login' && (
            <button type="button" className="text-link forgot" onClick={() => switchMode('forgot')}>Forgot password?</button>
          )}

          <div className="auth-switch">
            {mode === 'signup' ? 'Already registered?' : "Don't have an account?"}
            <button type="button" onClick={() => switchMode(mode === 'signup' ? 'login' : 'signup')}>
              {mode === 'signup' ? 'Sign in' : 'Create account'}
            </button>
          </div>

          {mode === 'forgot' && (
            <button type="button" className="text-link" onClick={() => switchMode('login')}>Back to sign in</button>
          )}
        </form>
      </div>
    </div>
  );
}

function Field({label,value,onChange,type='text',placeholder='',required=false,autoComplete='off',readOnly=false}){return <label className="field"><span>{label}{required&&' *'}</span><input type={type} value={value??''} placeholder={placeholder} onChange={e=>onChange(e.target.value)} required={required} autoComplete={autoComplete} readOnly={readOnly}/></label>}

function Select({label,value,onChange,options}){return <label className="field"><span>{label}</span><select value={value??''} onChange={e=>onChange(e.target.value)}>{options.map(o=><option key={o.value} value={o.value}>{o.label}</option>)}</select></label>}

function Section({title,subtitle,action,children}){return <div className="section"><div className="section-head"><div><h3>{title}</h3>{subtitle&&<p>{subtitle}</p>}</div>{action}</div>{children}</div>}

function Stat({label,value,icon:Icon,tone='teal'}){return <div className="stat-card"><div className={`stat-icon ${tone}`}><Icon size={19}/></div><div><small>{label}</small><strong>{value??'—'}</strong></div></div>}

function Empty({text='No records found'}){return <div className="empty"><Droplet size={24}/><b>{text}</b><span>Records will appear here when available.</span></div>}

function DataTable({columns,rows,actions,emptyText='No records found'}){return <div className="table-wrap"><table><thead><tr>{columns.map(c=><th key={c.key}>{c.label}</th>)}{actions&&<th>Actions</th>}</tr></thead><tbody>{rows.length?rows.map((r,i)=><tr key={r.id??i}>{columns.map(c=><td key={c.key}>{c.render?c.render(r):val(r,...(c.keys||[c.key]))}</td>)}{actions&&<td><div className="row-actions">{actions(r)}</div></td>}</tr>):<tr><td colSpan={columns.length+(actions?1:0)}><Empty text={emptyText}/></td></tr>}</tbody></table></div>}

function Dashboard({user,role,go,reload,toast}){

 const [data,setData]=useState({requests:[],donors:[],hospitals:[],banks:[],notifications:[]});
 const [loading,setLoading]=useState(true);
 const [pollTick,setPollTick]=useState(0);
 useEffect(()=>{const timer=window.setInterval(()=>setPollTick(value=>value+1),15000);return()=>window.clearInterval(timer)},[]);

 useEffect(()=>{
   let alive=true;
   setLoading(true);
   const requestJob = role==='ADMIN' ? api.allRequests()
     : ['USER','HOSPITAL'].includes(role) ? api.myRequests()
     : ['DONOR','BLOOD_BANK'].includes(role) ? api.requests()
     : Promise.resolve([]);
   const jobs=[
     requestJob.catch(()=>[]),
     role==='ADMIN'?api.allDonors().catch(()=>[]):['USER','HOSPITAL'].includes(role)?api.donors({bloodGroup:'',city:''}).catch(()=>[]):Promise.resolve([]),
     role==='ADMIN'?api.hospitals().catch(()=>[]):Promise.resolve([]),
     role==='ADMIN'?api.banks().catch(()=>[]):Promise.resolve([]),
     api.notifications().catch(()=>[])
   ];
   Promise.all(jobs).then(v=>{
     if(alive) setData({
       requests:normalizeList(v[0]),
       donors:normalizeList(v[1]),
       hospitals:normalizeList(v[2]),
       banks:normalizeList(v[3]),
       notifications:normalizeList(v[4])
     });
   }).finally(()=>{if(alive)setLoading(false)});
   return()=>{alive=false};
 },[role,reload,pollTick]);

 const canRequest=['USER','HOSPITAL'].includes(role);
 const greeting=user?.name?.trim()?.split(/\s+/)[0]||'there';
 const description=role==='USER'
   ? 'Raise an emergency blood request and connect with eligible donors around you.'
   : role==='HOSPITAL'
     ? 'Coordinate urgent blood requirements and reach compatible donors faster.'
     : role==='DONOR'
       ? 'Review nearby blood requests and keep your donor availability up to date.'
       : role==='BLOOD_BANK'
         ? 'Monitor urgent requirements, manage stock and support nearby patients.'
         : 'Monitor emergency requests, donor activity and the health of your network.';

 return <>
   <div className="welcome dashboard-hero">
     <div className="welcome-copy">
       <span className="eyebrow"><span className="live-dot"/> BLOODBRIDGE · EMERGENCY NETWORK</span>
       <h1>Good to see you, {greeting} <span>👋</span></h1>
       <p>{description}</p>
       <div className="hero-points"><span><HeartPulse size={14}/> Help when every minute matters</span><span><ShieldCheck size={14}/> Secure, role-based access</span></div>
       {canRequest&&<button className="btn-primary hero-cta" onClick={()=>go('requests')}><Plus size={17}/> Create blood request</button>}
     </div>
     <div className="hero-art" aria-hidden="true">
       <div className="hero-orbit orbit-one"/><div className="hero-orbit orbit-two"/>
       <div className="hero-drop"><Droplet size={43} fill="currentColor" strokeWidth={1.4}/></div>
       <div className="hero-art-note"><b>Every drop counts</b><small>Connecting people. Saving lives.</small></div>
     </div>
   </div>

   <div className="stats-grid">
     <Stat label={role==='USER'||role==='HOSPITAL'?'My requests':role==='ADMIN'?'All requests':'Incoming requests'} value={loading?'…':data.requests.length} icon={ClipboardList} tone="red"/>
     <Stat label="Available donors" value={loading?'…':data.donors.filter(d=>d.available!==false).length} icon={Users} tone="blue"/>
     <Stat label="Unread notifications" value={loading?'…':data.notifications.filter(n=>!n.readFlag).length} icon={Bell} tone="orange"/>
     <Stat label="Your account role" value={role.replace('_',' ')} icon={ShieldCheck} tone="purple"/>
   </div>

   <div className="dashboard-grid">
     <div className="dashboard-main-card">
       <Section title={role==='USER'||role==='HOSPITAL'?'My recent requests':role==='ADMIN'?'All recent requests':'Incoming blood requests'} subtitle="Requests visible to your account based on its role" action={<button className="text-link" onClick={()=>go('requests')}>View all <span>→</span></button>}/>
       <div className="request-list">
         {data.requests.slice(0,5).map((r,i)=><div className="request-row" key={r.id||i}>
           <div className="blood-badge">{r.bloodGroup||'?'}</div>
           <div className="request-info"><b>{val(r,'patientName','name')}</b><small><MapPin size={12}/> {val(r,'location')} <span className="dot-separator">·</span> {val(r,'unitsNeeded')} units</small></div>
           <Status value={r.status}/>
         </div>)}
         {!loading&&!data.requests.length&&<Empty text="No active blood requests"/>}
         {loading&&<div className="loading-line">Loading recent requests…</div>}
       </div>
     </div>
     <div className="quick-panel">
       <Section title="Quick actions" subtitle="Your shortcuts"/>
       {['USER','HOSPITAL','BLOOD_BANK','ADMIN'].includes(role)&&<button onClick={()=>go('donors')}><span className="quick-icon"><Search size={17}/></span><span>Find a blood donor</span><span className="quick-arrow">→</span></button>}
       {canRequest&&<button onClick={()=>go('requests')}><span className="quick-icon urgent"><AlertCircle size={17}/></span><span>Create / track requests</span><span className="quick-arrow">→</span></button>}
       {['DONOR','BLOOD_BANK'].includes(role)&&<button onClick={()=>go('requests')}><span className="quick-icon"><ClipboardList size={17}/></span><span>View incoming requests</span><span className="quick-arrow">→</span></button>}
       {role==='ADMIN'&&<button onClick={()=>go('requests')}><span className="quick-icon"><ShieldCheck size={17}/></span><span>Manage all requests</span><span className="quick-arrow">→</span></button>}
       {role==='BLOOD_BANK'?<button onClick={()=>go('inventory')}><span className="quick-icon"><Package size={17}/></span><span>Update blood inventory</span><span className="quick-arrow">→</span></button>:<button onClick={()=>go('profile')}><span className="quick-icon"><UserRound size={17}/></span><span>Complete your profile</span><span className="quick-arrow">→</span></button>}
       <button onClick={()=>go('notifications')}><span className="quick-icon"><Bell size={17}/></span><span>Check notifications</span><span className="quick-arrow">→</span></button>
     </div>
   </div>
 </>;
}

function Status({value}){const s=String(value||'PENDING').toUpperCase();return <span className={`status ${s.toLowerCase()}`}>{s.replace('_',' ')}</span>}

function Requests({role,user,toast,reload}){
  const isRequester = ['USER','HOSPITAL'].includes(role);
  const isResponder = ['DONOR','BLOOD_BANK'].includes(role);
  const isAdmin = role === 'ADMIN';
  const canCreate = isRequester;
  const initialTab = isAdmin ? 'all' : isResponder ? 'incoming' : 'mine';

  const [rows,setRows] = useState([]);
  const [mine,setMine] = useState([]);
  const [accepted,setAccepted] = useState([]);
  const [show,setShow] = useState(false);
  const [busy,setBusy] = useState(false);
  const [tab,setTab] = useState(initialTab);
  const [f,setF] = useState({patientName:'',bloodGroup:'O+',location:'',urgency:'HIGH',contactNumber:'',unitsNeeded:1});
  const set = (key,value) => setF(prev=>({...prev,[key]:value}));

  const load = useCallback(async()=>{
    try {
      if (isRequester) {
        // Requester / hospital account must never fetch the global request feed.
        const own = await api.myRequests();
        setMine(normalizeList(own));
        setRows([]);
        setAccepted([]);
      } else if (isResponder) {
        // Donors and verified blood banks receive pending incoming requests only.
        const [incoming, handled] = await Promise.all([
          api.requests(),
          api.acceptedRequests().catch(()=>[])
        ]);
        setRows(normalizeList(incoming));
        setAccepted(normalizeList(handled));
        setMine([]);
      } else if (isAdmin) {
        // Admin can monitor every request but cannot create or accept one.
        const all = await api.allRequests();
        setRows(normalizeList(all));
        setMine([]);
        setAccepted([]);
      } else {
        setRows([]); setMine([]); setAccepted([]);
      }
    } catch(error) {
      toast(error.message || 'Unable to load blood requests.','error');
    }
  },[toast,isRequester,isResponder,isAdmin]);

  useEffect(()=>{
    load();
    if (!isResponder) return undefined;
    const timer=window.setInterval(load,15000);
    return ()=>window.clearInterval(timer);
  },[load,isResponder]);

  const submit = async (event) => {
    event.preventDefault();
    if (busy || !canCreate) return;
    const payload={
      patientName:f.patientName.trim(),
      bloodGroup:f.bloodGroup,
      location:f.location.trim(),
      urgency:f.urgency,
      contactNumber:f.contactNumber.trim(),
      unitsNeeded:Number(f.unitsNeeded)
    };
    if(!payload.patientName||!payload.bloodGroup||!payload.location||!payload.contactNumber){
      toast('Please fill all required request details.','error');
      return;
    }
    if(!Number.isInteger(payload.unitsNeeded)||payload.unitsNeeded<1){
      toast('Units needed must be a whole number greater than 0.','error');
      return;
    }
    setBusy(true);
    try {
      await api.addRequest(payload);
      toast('Request submitted. Matching donors and verified blood banks will be notified.');
      setF({patientName:'',bloodGroup:'O+',location:'',urgency:'HIGH',contactNumber:'',unitsNeeded:1});
      setShow(false);
      await load();
      reload();
    } catch(error) {
      toast(error.message || 'Unable to create blood request.','error');
    } finally {
      setBusy(false);
    }
  };

  const act = async (id,action) => {
    try {
      await api.requestAction(id,action);
      toast(action==='accept'?'Request accepted successfully.':action==='fulfill'?'Request marked as fulfilled.':'Request canceled.');
      await load();
      reload();
    } catch(error) {
      toast(error.message || 'Unable to update request.','error');
    }
  };

  const list = isRequester ? mine : isAdmin ? rows : tab==='accepted' ? accepted : rows;
  const title = isRequester ? 'My blood requests' : isAdmin ? 'All blood requests' : tab==='accepted' ? 'Accepted by me' : 'Incoming blood requests';
  const subtitle = isRequester
    ? 'Only requests created by your account appear here. Check whether a donor or blood bank has accepted.'
    : isAdmin
      ? 'View and manage all user and hospital requests. Admin accounts cannot create or accept requests.'
      : tab==='accepted'
        ? 'Requests that you have accepted.'
        : 'Pending requests routed to donors and verified blood banks. Accept a request when you can help.';

  return <>
    <Section title={title} subtitle={subtitle} action={canCreate&&<button className="btn-primary" onClick={()=>setShow(v=>!v)}><Plus size={16}/> New request</button>}/>

    {show&&canCreate&&<form className="form-card" onSubmit={submit}>
      <h4>New emergency blood request</h4>
      <div className="form-grid">
        <Field label="Patient name" value={f.patientName} onChange={v=>set('patientName',v)} required/>
        <Select label="Blood group" value={f.bloodGroup} onChange={v=>set('bloodGroup',v)} options={GROUPS.map(x=>({value:x,label:x}))}/>
        <Field label="Hospital / location" value={f.location} onChange={v=>set('location',v)} required/>
        <Field label="Contact number" value={f.contactNumber} onChange={v=>set('contactNumber',v)} required/>
        <Select label="Urgency" value={f.urgency} onChange={v=>set('urgency',v)} options={['LOW','MEDIUM','HIGH','CRITICAL'].map(x=>({value:x,label:x}))}/>
        <Field label="Units needed" type="number" value={f.unitsNeeded} onChange={v=>set('unitsNeeded',v)} required/>
      </div>
      <div className="form-actions">
        <button type="button" className="btn-outline" onClick={()=>setShow(false)}>Cancel</button>
        <button className="btn-primary" disabled={busy}>{busy?'Submitting…':'Submit request'}</button>
      </div>
    </form>}

    {isResponder&&<div className="tabs">
      <button className={tab==='incoming'?'selected':''} onClick={()=>setTab('incoming')}>Incoming requests</button>
      <button className={tab==='accepted'?'selected':''} onClick={()=>setTab('accepted')}>Accepted by me</button>
    </div>}
    {isRequester&&<div className="request-owner-note"><ClipboardList size={16}/> Only your requests are shown here. Their status will change when a donor or blood bank accepts.</div>}
    {isAdmin&&<div className="request-owner-note"><ShieldCheck size={16}/> Admin can view and manage all requests. Creating and accepting requests are disabled.</div>}

    <DataTable columns={[
      {key:'bloodGroup',label:'Blood group',render:r=><b className="group-text">{r.bloodGroup}</b>},
      {key:'patientName',label:'Patient'},
      {key:'location',label:'Location'},
      {key:'unitsNeeded',label:'Units'},
      {key:'urgency',label:'Urgency'},
      {key:'status',label:'Status',render:r=><Status value={r.status}/>},
      {key:'createdAt',label:'Created',render:r=>fmt(r.createdAt)}
    ]} rows={list} emptyText={isRequester?'You have not created any blood requests yet.':isAdmin?'No blood requests are present.':role==='DONOR'?'No matching requests. Your donor profile must be verified, available and eligible.':'No matching requests. Your blood bank must be verified and have enough compatible stock.'} actions={r=><>
      {isResponder&&tab==='incoming'&&r.status==='PENDING'&&<button className="small-action" onClick={()=>act(r.id,'accept')}>Accept</button>}
      {(isRequester||isAdmin)&&r.status==='ACCEPTED'&&<button className="small-action" onClick={()=>act(r.id,'fulfill')}>Fulfill</button>}
      {(isRequester||isAdmin)&&['PENDING','ACCEPTED'].includes(r.status)&&<button className="small-action danger" onClick={()=>act(r.id,'cancel')}>Cancel</button>}
    </>}/>
  </>;
}

function Donors({role,toast,reload}){

 const [rows,setRows]=useState([]),[q,setQ]=useState({bloodGroup:'',city:''}),[form,setForm]=useState({name:'',bloodGroup:'O+',city:'',phone:'',email:'',address:'',lastDonationDate:'',age:'',weight:''}),[show,setShow]=useState(false),[mine,setMine]=useState(null);

 const load=useCallback(async()=>{try{let d=role==='ADMIN'?await api.allDonors():await api.donors(q);setRows(normalizeList(d));}catch(e){toast(e.message,'error')}},[role,q,toast]);useEffect(()=>{load()},[load]);useEffect(()=>{if(role==='DONOR')api.donorMe().then(setMine).catch(()=>setMine(null))},[role]);

 const submit=async e=>{e.preventDefault();try{await api.addDonor({...form,age:Number(form.age),weight:Number(form.weight),lastDonationDate:form.lastDonationDate||null});toast('Donor profile created');setShow(false);load();reload()}catch(e){toast(e.message,'error')}};

 const verify=async(id,a)=>{try{await api.donorVerify(id,a);toast(`Donor ${a}ed`);load()}catch(e){toast(e.message,'error')}};

 return <><Section title="Donor directory" subtitle="Search eligible donors by blood group and city" action={role==='DONOR'&&!mine?<button className="btn-primary" onClick={()=>setShow(!show)}><Plus size={16}/> Create donor profile</button>:null}/>{role==='DONOR'&&mine&&<div className="notice-card">Your donor profile is registered · Availability: <b>{mine.available?'Available':'Unavailable'}</b><button className="btn-outline" onClick={async()=>{try{await api.donorAvailability(mine.id,!mine.available);const d=await api.donorMe();setMine(d);toast('Availability updated')}catch(e){toast(e.message,'error')}}}>Set {mine.available?'unavailable':'available'}</button></div>}{show&&<form className="form-card" onSubmit={submit}><h4>Donor profile</h4><div className="form-grid">{[['name','Full name'],['city','City'],['phone','Phone'],['address','Address'],['age','Age'],['weight','Weight (kg)'],['lastDonationDate','Last donation date']].map(([k,l])=><Field key={k} label={l} type={k==='lastDonationDate'?'date':k==='age'||k==='weight'?'number':'text'} value={form[k]} onChange={v=>setForm(p=>({...p,[k]:v}))} required={['name','city','phone','weight'].includes(k)}/>)}<Select label="Blood group" value={form.bloodGroup} onChange={v=>setForm(p=>({...p,bloodGroup:v}))} options={GROUPS.map(x=>({value:x,label:x}))}/></div><div className="form-actions"><button type="button" className="btn-outline" onClick={()=>setShow(false)}>Cancel</button><button className="btn-primary">Save donor profile</button></div></form>}<div className="filter-bar"><Select label="Blood group" value={q.bloodGroup} onChange={v=>setQ(p=>({...p,bloodGroup:v}))} options={[{value:'',label:'All groups'},...GROUPS.map(x=>({value:x,label:x}))]}/><Field label="City" value={q.city} onChange={v=>setQ(p=>({...p,city:v}))} placeholder="Search city"/><button className="btn-outline" onClick={load}><Search size={15}/> Search</button></div><DataTable columns={[{key:'name',label:'Donor'},{key:'bloodGroup',label:'Blood group',render:r=><b className="group-text">{r.bloodGroup}</b>},{key:'city',label:'City'},{key:'phone',label:'Phone'},{key:'available',label:'Availability',render:r=><Status value={r.available?'AVAILABLE':'UNAVAILABLE'}/>},{key:'verificationStatus',label:'Verification',render:r=><Status value={r.verificationStatus}/>},{key:'lastDonationDate',label:'Last donation'}]} rows={rows} actions={role==='ADMIN'?r=><><button className="small-action" onClick={()=>verify(r.id,'verify')}>Verify</button><button className="small-action danger" onClick={()=>verify(r.id,'reject')}>Reject</button></>:null}/></>;

}

function Hospitals({role,toast}){

 const [rows,setRows]=useState([]),[city,setCity]=useState(''),[show,setShow]=useState(false),[f,setF]=useState({name:'',contactNumber:'',registrationNumber:'',address:'',city:''});

 const load=useCallback(()=>{if(role==='ADMIN'||city.trim())api.hospitals(city).then(x=>setRows(normalizeList(x))).catch(e=>toast(e.message,'error'));else setRows([])},[city,role,toast]);useEffect(()=>{load()},[load]);

 const submit=async e=>{e.preventDefault();try{await api.registerHospital(f);toast('Hospital registered. Pending admin verification.');setShow(false);load()}catch(e){toast(e.message,'error')}};

 const act=async(id,a)=>{try{await api.hospitalVerify(id,a);toast(`Hospital ${a}ed`);load()}catch(e){toast(e.message,'error')}};

 return <><Section title="Hospitals" subtitle="Find registered hospitals or submit a hospital profile" action={role==='HOSPITAL'&&<button className="btn-primary" onClick={()=>setShow(!show)}><Plus size={16}/> Register hospital</button>}/>{show&&<form className="form-card" onSubmit={submit}><h4>Hospital registration</h4><div className="form-grid">{[['name','Hospital name'],['contactNumber','Contact number'],['registrationNumber','Registration number'],['address','Address'],['city','City']].map(([k,l])=><Field key={k} label={l} value={f[k]} onChange={v=>setF(p=>({...p,[k]:v}))} required/>)}</div><button className="btn-primary">Submit registration</button></form>}<div className="filter-bar"><Field label="Filter by city" value={city} onChange={setCity} placeholder="Enter city"/><button className="btn-outline" onClick={load}><Search size={15}/> Search</button></div><DataTable columns={[{key:'name',label:'Hospital'},{key:'city',label:'City'},{key:'address',label:'Address'},{key:'contactNumber',label:'Contact'},{key:'registrationNumber',label:'Registration no.'},{key:'verificationStatus',label:'Verification',render:r=><Status value={r.verificationStatus}/> }]} rows={rows} actions={role==='ADMIN'?r=><><button className="small-action" onClick={()=>act(r.id,'verify')}>Verify</button><button className="small-action danger" onClick={()=>act(r.id,'reject')}>Reject</button></>:null}/></>;

}

function Banks({role,toast}){

 const [rows,setRows]=useState([]),[city,setCity]=useState(''),[show,setShow]=useState(false),[f,setF]=useState({name:'',contactNumber:'',licenseNumber:'',address:'',city:''});

 const load=useCallback(()=>{if(role==='ADMIN'||city.trim())api.banks(city).then(x=>setRows(normalizeList(x))).catch(e=>toast(e.message,'error'));else setRows([])},[city,role,toast]);useEffect(()=>{load()},[load]);

 const submit=async e=>{e.preventDefault();try{await api.registerBank(f);toast('Blood bank registered. Pending verification.');setShow(false);load()}catch(e){toast(e.message,'error')}};

 const act=async(id,a)=>{try{await api.bankVerify(id,a);toast(`Blood bank ${a}ed`);load()}catch(e){toast(e.message,'error')}};

 return <><Section title="Blood banks" subtitle="Search blood banks and check their verification status" action={role==='BLOOD_BANK'&&<button className="btn-primary" onClick={()=>setShow(!show)}><Plus size={16}/> Register blood bank</button>}/>{show&&<form className="form-card" onSubmit={submit}><h4>Blood bank registration</h4><div className="form-grid">{[['name','Blood bank name'],['contactNumber','Contact number'],['licenseNumber','License number'],['address','Address'],['city','City']].map(([k,l])=><Field key={k} label={l} value={f[k]} onChange={v=>setF(p=>({...p,[k]:v}))} required/>)}</div><button className="btn-primary">Submit registration</button></form>}<div className="filter-bar"><Field label="Filter by city" value={city} onChange={setCity} placeholder="Enter city"/><button className="btn-outline" onClick={load}><Search size={15}/> Search</button></div><DataTable columns={[{key:'name',label:'Blood bank'},{key:'city',label:'City'},{key:'address',label:'Address'},{key:'contactNumber',label:'Contact'},{key:'licenseNumber',label:'License no.'},{key:'verificationStatus',label:'Verification',render:r=><Status value={r.verificationStatus}/> }]} rows={rows} actions={role==='ADMIN'?r=><><button className="small-action" onClick={()=>act(r.id,'verify')}>Verify</button><button className="small-action danger" onClick={()=>act(r.id,'reject')}>Reject</button></>:null}/></>;

}

function Inventory({toast,role}){

 const [rows,setRows]=useState([]),[q,setQ]=useState({bloodGroup:'A+',unitsAvailable:'0'}),[city,setCity]=useState('');

 const load=useCallback(()=>{const job=role==='ADMIN'?api.stockSearch(q.bloodGroup,city):api.bankStock();job.then(x=>setRows(normalizeList(x))).catch(e=>toast(e.message,'error'))},[toast,role,q.bloodGroup,city]);useEffect(()=>{if(role!=='ADMIN')load()},[load,role]);

 const save=async e=>{e.preventDefault();try{await api.updateStock({...q,unitsAvailable:Number(q.unitsAvailable)});toast('Inventory updated');load()}catch(e){toast(e.message,'error')}};

 return <><Section title="Blood inventory" subtitle={role==='ADMIN'?'Search available blood stock across registered blood banks':'Manage available blood units for your registered blood bank'}/>{role==='ADMIN'?<div className="filter-bar"><Select label="Blood group" value={q.bloodGroup} onChange={v=>setQ(p=>({...p,bloodGroup:v}))} options={GROUPS.map(x=>({value:x,label:x}))}/><Field label="City" value={city} onChange={setCity} placeholder="Enter city"/><button className="btn-outline" onClick={load}><Search size={15}/> Search stock</button></div>:null}<div className="inventory-layout">{role==='BLOOD_BANK'&&<form className="form-card" onSubmit={save}><h4>Update stock</h4><Select label="Blood group" value={q.bloodGroup} onChange={v=>setQ(p=>({...p,bloodGroup:v}))} options={GROUPS.map(x=>({value:x,label:x}))}/><Field label="Available units" type="number" value={q.unitsAvailable} onChange={v=>setQ(p=>({...p,unitsAvailable:v}))} required/><button className="btn-primary full">Save inventory</button></form>}<div className="inventory-card"><div className="inventory-head"><Package/><div><b>{role==='ADMIN'?'Blood stock search results':'My blood stock'}</b><small>{role==='ADMIN'?'Stock levels reported by blood banks':'Current recorded units'}</small></div></div><div className="stock-grid">{rows.map((r,i)=><div className="stock-item" key={r.id||i}><span>{r.bloodGroup}</span><b>{val(r,'unitsAvailable','units')}</b><small>{val(r,'bloodBankName','name','city')}</small></div>)}</div>{!rows.length&&<Empty text={role==='ADMIN'?'Search by blood group and city':'No stock entries yet'}/>}</div></div></>;

}

function Notifications({toast,onChange}){const [rows,setRows]=useState([]);const load=useCallback(()=>api.notifications().then(x=>setRows(normalizeList(x))).catch(e=>toast(e.message,'error')),[toast]);useEffect(()=>{load();const timer=window.setInterval(load,20000);return()=>window.clearInterval(timer)},[load]);const read=async id=>{try{await api.markRead(id);load();onChange()}catch(e){toast(e.message,'error')}};const all=async()=>{try{await api.markAllRead();toast('All notifications marked as read');load();onChange()}catch(e){toast(e.message,'error')}};return <><Section title="Notifications" subtitle="Updates about requests, donations and account activity" action={<button className="btn-outline" onClick={all}><Check size={15}/> Mark all read</button>}/><div className="notification-list">{rows.map((r,i)=><div className={`notification-row ${r.readFlag?'read':''}`} key={r.id||i}><div className="notification-icon"><Bell size={17}/></div><div className="notification-body"><b>{r.type||'Update'}</b><p>{r.message}</p><small>{fmt(r.createdAt)}</small></div>{!r.readFlag&&<button className="small-action" onClick={()=>read(r.id)}>Mark read</button>}</div>)}{!rows.length&&<Empty text="You’re all caught up"/>}</div></>}

function Profile({user,setUser,toast,logout}){const [f,setF]=useState({name:user?.name||'',phone:user?.phone||'',gender:user?.gender||'',profession:user?.profession||'',location:user?.location||''}),[pass,setPass]=useState({oldPassword:'',newPassword:''}),[busy,setBusy]=useState(false);useEffect(()=>{api.me().then(x=>{setUser(x);setF({name:x.name||'',phone:x.phone||'',gender:x.gender||'',profession:x.profession||'',location:x.location||''})}).catch(()=>{})},[setUser]);const save=async e=>{e.preventDefault();setBusy(true);try{const r=await api.updateMe(f);setUser(r);toast('Profile updated')}catch(e){toast(e.message,'error')}finally{setBusy(false)}};const change=async e=>{e.preventDefault();try{await api.changePassword(pass);setPass({oldPassword:'',newPassword:''});toast('Password changed')}catch(e){toast(e.message,'error')}};return <><Section title="My profile" subtitle="Manage your personal information and security"/><div className="profile-grid"><form className="form-card" onSubmit={save}><h4>Personal information</h4><div className="form-grid">{[['name','Full name'],['phone','Phone'],['gender','Gender'],['profession','Profession'],['location','City / location']].map(([k,l])=><Field key={k} label={l} value={f[k]} onChange={v=>setF(p=>({...p,[k]:v}))}/>)}</div><Field label="Account email" type="email" value={user?.email||''} onChange={()=>{}} readOnly autoComplete="off"/><button className="btn-primary" disabled={busy}>Save profile</button></form><div className="form-card"><h4>Account security</h4><form onSubmit={change}><Field label="Current password" type="password" value={pass.oldPassword} onChange={v=>setPass(p=>({...p,oldPassword:v}))} required/><Field label="New password" type="password" value={pass.newPassword} onChange={v=>setPass(p=>({...p,newPassword:v}))} required/><button className="btn-primary">Change password</button></form><div className="danger-zone"><b>Sign out</b><p>End your current session on this device.</p><button className="btn-outline danger-outline" onClick={logout}>Sign out</button><button className="btn-outline danger-outline" style={{marginLeft:8}} onClick={async()=>{if(window.confirm('Permanently delete your account? This cannot be undone.')){try{await api.deleteAccount();toast('Account deleted');logout()}catch(e){toast(e.message,'error')}}}}>Delete account</button></div></div></div></>}

function Admin({toast}){const [rows,setRows]=useState([]),[role,setRole]=useState('');const load=useCallback(()=>api.users(role).then(x=>setRows(normalizeList(x))).catch(e=>toast(e.message,'error')),[role,toast]);useEffect(()=>{load()},[load]);const action=async(id,a)=>{try{await api.userAction(id,a);toast(`User ${a}ed`);load()}catch(e){toast(e.message,'error')}};return <><Section title="User management" subtitle="Review registered accounts and manage access"/><div className="filter-bar"><Select label="Filter role" value={role} onChange={setRole} options={[{value:'',label:'All roles'},...['USER','DONOR','HOSPITAL','BLOOD_BANK','ADMIN'].map(x=>({value:x,label:x}))]}/></div><DataTable columns={[{key:'name',label:'Name'},{key:'email',label:'Email'},{key:'phone',label:'Phone'},{key:'role',label:'Role'},{key:'active',label:'Account',render:r=><Status value={r.active?'ACTIVE':'BLOCKED'}/>}]} rows={rows} actions={r=>r.role!=='ADMIN'&&<button className={`small-action ${r.active?'danger':''}`} onClick={()=>action(r.id,r.active?'ban':'unban')}>{r.active?'Block':'Unblock'}</button>}/></>}

export default App;
