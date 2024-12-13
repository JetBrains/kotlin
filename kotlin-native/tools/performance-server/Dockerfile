FROM node:latest

# Create app directory

WORKDIR /usr/src/app

# Install app dependencies
# A wildcard is used to ensure both package.json AND package-lock.json are copied
# where available (npm@5+)

COPY build/js/packages/app/package.json .
COPY kotlin-js-store/yarn.lock .
RUN yarn install

USER 1000

# If you are building your code for production
# RUN npm install --only=production
# Bundle app source

COPY ui/css ui/css
COPY ui/js ui/js
COPY ui/index.ejs ui
COPY server server

EXPOSE 3000
CMD [ "node", "server/app.js" ]