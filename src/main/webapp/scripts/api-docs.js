// Shows the OpenAPI document that Payara builds from the annotations of the REST classes.
window.addEventListener('load', () => {
  const ctx = document.body.dataset.ctx || '';
  SwaggerUIBundle({
    url: `${ctx}/openapi?format=json`,
    dom_id: '#swagger',
    deepLinking: true,
    tryItOutEnabled: false
  });
});
