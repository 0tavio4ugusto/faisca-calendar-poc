# POC - Google Calendar API + Faisca Arquitetura

## Passo 1: Configurar Google Cloud Console

1. Acesse [console.cloud.google.com](https://console.cloud.google.com)
2. Clique em **Select a project** → **New Project**
   - Nome: `Faisca Calendar POC`
   - Clique em **Create**
3. Com o projeto selecionado, va em **APIs & Services** → **Library**
4. Busque por **Google Calendar API** → clique → **Enable**
5. Va em **APIs & Services** → **OAuth consent screen**
   - User Type: **External** → Create
   - App name: `Faisca POC`
   - User support email: seu email
   - Developer contact: seu email
   - Save and Continue (nao precisa preencher scopes)
   - Save and Continue
6. Em **Test users**, clique **Add users** → coloque seu email do Google → Save
7. Va em **APIs & Services** → **Credentials**
   - **Create Credentials** → **OAuth client ID**
   - Application type: **Web application**
   - Name: `Faisca POC`
   - Authorized redirect URIs: **Add URI** → `http://localhost:8080/api/calendar/callback`
   - **Create**
8. Copie o **Client ID** e **Client Secret**

## Passo 2: Configurar o Backend

1. Abra `backend/src/main/resources/application.properties`
2. Cole o Client ID e Client Secret:
```
google.client.id=SEU_CLIENT_ID_AQUI.apps.googleusercontent.com
google.client.secret=SEU_CLIENT_SECRET_AQUI
```

## Passo 3: Rodar o Backend

```bash
cd backend
./mvnw spring-boot:run
```

Ou no Windows com Maven instalado:
```bash
cd backend
mvn spring-boot:run
```

Backend rodando em: http://localhost:8080

## Passo 4: Rodar o Frontend

Em outro terminal:
```bash
cd frontend
npm install
npm run dev
```

Frontend rodando em: http://localhost:5173

## Passo 5: Testar

1. Abra http://localhost:5173
2. Clique em **Conectar Google Calendar**
3. Faca login com sua conta Google
4. Autorize o acesso
5. Volta pro app com status "Conectado"
6. Clique **Criar Evento de Teste**
7. Clique **Listar Eventos**