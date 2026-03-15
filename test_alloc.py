import requests
from bs4 import BeautifulSoup

session = requests.Session()
# Get login page to get CSRF
login_page = session.get('http://localhost:8080/login')
soup = BeautifulSoup(login_page.text, 'html.parser')
csrf_token = soup.find('input', {'name': '_csrf'})['value']

# Login as admin
login_data = {
    'username': 'admin@example.com',
    'password': 'password',
    '_csrf': csrf_token
}
resp = session.post('http://localhost:8080/login', data=login_data)
print("Login status:", resp.status_code)

# Fetch allocation requests
alloc_resp = session.get('http://localhost:8080/admin/allocation-requests')
print("Alloc page status:", alloc_resp.status_code)
if alloc_resp.status_code != 200:
    print("Error page content:")
    soup2 = BeautifulSoup(alloc_resp.text, 'html.parser')
    print(soup2.text)
else:
    print("Success! Page rendered.")
