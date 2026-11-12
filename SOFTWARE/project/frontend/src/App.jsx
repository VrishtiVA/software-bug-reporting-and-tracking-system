//Imports
import { useEffect } from "react";
import { Routes, Route, useNavigate, Navigate } from "react-router-dom";

import { AuthenticationProvider } from "./authentication/AuthenticationContext";
import { ErrorProvider } from "./components/contexts/ErrorContext";
import { RequireAuthentication } from "./authentication/RequireAuthentication";
import { SnackbarProvider } from "./components/contexts/SnackbarContext";
import { setNavigationMethodInAxiosJS } from "./api/axios";

//Layouts
import CentrePageLayout from "./styles/CentrePageLayout";
import NavigationSidebarAppbarPageLayout from "./styles/NavigationSidebarAppbarPageLayout";

//Pages
import ErrorPage from "./pages/ErrorPage";
import SigninPage from "./pages/users/SigninPage";
import SignupPage from "./pages/users/SignupPage";
import WorkspacesPage from "./pages/workspaces/WorkspacesPage";
import WorkspacePage from "./pages/workspaces/WorkspacePage";
import AppbarPageLayout from "./styles/AppBarPageLayout";
import CreateWorkspacePage from "./pages/workspaces/CreateWorkspacePage";
import ConfigureWorkspacePage from "./pages/workspaces/ConfigureWorkspacePage";
import CreateBugReportPage from "./pages/reports/CreateBugReportPage";
import ResolveDuplicateReportsPage from "./pages/reports/ResolveDuplicateReportsPage";
import BugPage from "./pages/bugs/BugPage";
import BugAuditLogPage from "./pages/bugs/BugAuditLogPage";
import AssignmentsPage from "./pages/bugs/AssignmentsPage";
import UserReportsPage from "./pages/reports/UserReportsPage";
import CatchUpPage from "./pages/users/CatchUpPage";

function App() {

  const navigate = useNavigate();
  useEffect(() => {
    setNavigationMethodInAxiosJS(navigate)
  }, [navigate])

  return (
    <SnackbarProvider><ErrorProvider><AuthenticationProvider>
      <Routes>
        <Route path="/" element={
          <Navigate to="/workspaces" replace/>
        } />

        {/* Authentication Routes */}
        <Route path="/signin" element={
          <CentrePageLayout>
            <SigninPage />
          </CentrePageLayout>
        } />
        <Route path="/signup" element={
          <CentrePageLayout>
            <SignupPage />
          </CentrePageLayout>
        } />
        <Route path="/dashboard" element={
          <Navigate to="/workspaces" replace/>
        }></Route>

        {/* Workspace Routes */}
        <Route path="/workspaces" element={
          <RequireAuthentication>
            <NavigationSidebarAppbarPageLayout>
              <WorkspacesPage />
            </NavigationSidebarAppbarPageLayout>
          </RequireAuthentication>
        } />
        <Route path="/workspaces/create" element={
          <RequireAuthentication>
            <NavigationSidebarAppbarPageLayout>
              <CreateWorkspacePage />
            </NavigationSidebarAppbarPageLayout>
          </RequireAuthentication>
        } />
        <Route path="/workspaces/:id" element={
          <RequireAuthentication>
            <NavigationSidebarAppbarPageLayout>
              <WorkspacePage />
            </NavigationSidebarAppbarPageLayout>
          </RequireAuthentication>
        } />
        <Route path="/workspaces/:id/configure" element={
          <RequireAuthentication>
            <NavigationSidebarAppbarPageLayout>
              <ConfigureWorkspacePage />
            </NavigationSidebarAppbarPageLayout>
          </RequireAuthentication>
        } />

        {/* Bug Assignment View Routes */}
        <Route path="/workspaces/:id/bugs/assignments" element={
          <RequireAuthentication>
            <NavigationSidebarAppbarPageLayout>
              <AssignmentsPage />
            </NavigationSidebarAppbarPageLayout>
          </RequireAuthentication>
        } />
        <Route path="/bugs/assignments" element={
          <RequireAuthentication>
            <NavigationSidebarAppbarPageLayout>
              <AssignmentsPage />
            </NavigationSidebarAppbarPageLayout>
          </RequireAuthentication>
        } />

        {/* User Reports View Routes */}
        <Route path="/reports" element={
          <RequireAuthentication>
            <NavigationSidebarAppbarPageLayout>
              <UserReportsPage />
            </NavigationSidebarAppbarPageLayout>
          </RequireAuthentication>
        } />

        {/* Bug Report Routes */}
        <Route path="/workspaces/:id/bugs/report" element={
          <RequireAuthentication>
            <NavigationSidebarAppbarPageLayout>
              <CreateBugReportPage/>
            </NavigationSidebarAppbarPageLayout>
          </RequireAuthentication>
        } />
        <Route path="/workspaces/:id/bugs/:bug_id" element={
          <RequireAuthentication>
            <NavigationSidebarAppbarPageLayout>
              <BugPage/>
            </NavigationSidebarAppbarPageLayout>
          </RequireAuthentication>
        } />
        <Route path="/workspaces/:id/bugs/:bug_id/audit-log" element={
          <RequireAuthentication>
            <NavigationSidebarAppbarPageLayout>
              <BugAuditLogPage/>
            </NavigationSidebarAppbarPageLayout>
          </RequireAuthentication>
        } />
        <Route path="/workspaces/:id/bugs/reports/duplicates/resolve" element={
          <RequireAuthentication>
            <NavigationSidebarAppbarPageLayout>
              <ResolveDuplicateReportsPage />
            </NavigationSidebarAppbarPageLayout>
          </RequireAuthentication>
        } />
        <Route path="/workspaces/:id/bugs/:bug_id/reports/duplicates/resolve" element={
          <RequireAuthentication>
            <NavigationSidebarAppbarPageLayout>
              <ResolveDuplicateReportsPage />
            </NavigationSidebarAppbarPageLayout>
          </RequireAuthentication>
        } />

        {/* Catch-up View Routes */}
        <Route path="/notifications" element={
          <RequireAuthentication>
            <NavigationSidebarAppbarPageLayout>
              <CatchUpPage />
            </NavigationSidebarAppbarPageLayout>
          </RequireAuthentication>
        } />

        {/* Catch all other pages e.g. for /error */}
        <Route path="*" element={
          <AppbarPageLayout>
            <ErrorPage />
          </AppbarPageLayout>
        }/>
      </Routes>
    </AuthenticationProvider></ErrorProvider></SnackbarProvider>
  )
}

export default App
